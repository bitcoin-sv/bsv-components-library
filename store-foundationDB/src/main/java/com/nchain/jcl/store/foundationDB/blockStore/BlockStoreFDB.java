package com.nchain.jcl.store.foundationDB.blockStore;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.google.common.collect.Lists;
import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockStore.BlockStore;
import com.nchain.jcl.store.blockStore.BlocksCompareResult;
import com.nchain.jcl.store.blockStore.events.*;
import com.nchain.jcl.store.foundationDB.common.FDBSafeIterator;
import com.nchain.jcl.store.foundationDB.common.HashesList;

import com.nchain.jcl.store.foundationDB.common.KeyValueUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.nchain.jcl.store.foundationDB.common.KeyValueUtils.*;
import static com.nchain.jcl.store.foundationDB.common.KeyValueUtils.toHashes;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of the BlockStore interface using FoundationDB.
 * NOTE:
 * In the current version, this implemenattion makes use of the DIRECTORY LAYER of FoundationDB to structure the
 * Data withint the DB. Future versions will include the RECORD LALER as well..
 */
@Slf4j
public class BlockStoreFDB implements BlockStore {

    // Configuration
    private BlockStoreFDBConfig config;
    private final boolean triggerBlockEvents;
    private final boolean triggerTxEvents;

    // DB Connection:
    @Getter protected FDB fdb;
    @Getter protected Database db;

    // Directories within the DB:
    protected DirectoryLayer dirLayer;
    @Getter protected DirectorySubspace netDir;
    @Getter protected DirectorySubspace blockchainDir;
    @Getter protected DirectorySubspace blocksDir;
    @Getter protected DirectorySubspace txsDir;

    // Events Streaming Configuration:
    protected static final int MAX_EVENT_ITEMS = 1000;  // max of items published on each Event
    protected final ExecutorService executorService;
    protected final EventBus eventBus;
    private final BlockStoreStreamer blockStoreStreamer;

    /** Constructor */
    /**
     * Constructor. It initializes the Block Store Implementation. This method initializes the internal state of this
     * Class, but it does NOT start the Connection to the DB. Remember to call the {@link #start()} method before start using
     * this class.
     *
     * @param config                Configuration class for the FoundationDB Implementation
     * @param triggerBlockEvents    Indicates if the Events regarding Blocks are being triggered. If so, you'll need to
     *                              subscribe to those Events by using the "EVENTS()" method.
     * @param triggerTxEvents       Indicates if the Events regarding Txs are being triggered.
     */
    @Builder
    public BlockStoreFDB(BlockStoreFDBConfig config, boolean triggerBlockEvents, boolean triggerTxEvents) {
        this.config = config;
        this.triggerBlockEvents = triggerBlockEvents;
        this.triggerTxEvents = triggerTxEvents;

        // Events Configuration:
        this.executorService = ThreadUtils.getThreadPoolExecutorService("BlockStore-LevelDB");
        this.eventBus = EventBus.builder().executor(this.executorService).build();
        this.blockStoreStreamer = new BlockStoreStreamer(this.eventBus);
    }

    /*
        Basic Operations:
        These are very low-level DB operations that are wrapped up in the methods below, so Exceptions are only
        captured once, etc.
     */

    // It stores a Key/Value record in the DB
    private void save(Transaction tr, byte[] key, byte[] value) {
        tr.set(key, value);
    }

    // It removes a Record from the DB
    private void remove(Transaction tr, byte[] key) {
        tr.clear(key);
    }

    // It reads a Record from the DB, returns NULL if Not found.
    private byte[] read(Transaction tr, byte[] key) {
        try {
              return tr.get(key).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // It opens and returns a Directory inside a Parent Directory:
    private DirectorySubspace openDir(Transaction tr, DirectorySubspace parentDir, String dirName) {
        try {
            return parentDir.createOrOpen(tr, Arrays.asList(dirName)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /*
        Block Store DB Operations:
        These methods execute the business logic. Most of the time, each one of the methods below map a method of the
        BlockStore interface, but with some peculiarities:
         - They do NOT trigger Events
         - They do NOT crete new DB Transaction, instead they need to reuse one passed as a parameter.

         The Events and Transactions are created at a higher-level (byt he public methods that implemen the BlockStore
         interface).
     */

    // It returns the reference to a Directory
    private DirectorySubspace _openDir(DirectorySubspace parentDir, String dirName) {
        AtomicReference<DirectorySubspace> result = new AtomicReference<>();
        db.run(tr -> {
            result.set(openDir(tr, parentDir, dirName));
            return null;
        });
        return result.get();
    }

    // It inserts the Whole Block and the "numTxs" property:
    private void _saveBlock(Transaction tr, BlockHeader blockHeader) {
        String blockHash = blockHeader.getHash().toString();
        save(tr, key(blocksDir, keyForBlock(blockHash)), bytes(blockHeader));
        save(tr, key(blocksDir, keyForBlockNumTxs(blockHash)), bytes(blockHeader.getNumTxs()));
    }

    // It inserts several Blocks
    private void _saveBlocks(Transaction tr, List<BlockHeader> blockHeaders) {
        blockHeaders.forEach(b -> _saveBlock(tr, b));
    }

    // It removes the whole Block, and all its properties related and stored separately;
    private void _removeBlock(Transaction tr, String blockHash) {
        remove(tr, key(blocksDir, keyForBlock(blockHash)));
        remove(tr, key(blocksDir, keyForBlockNumTxs(blockHash)));
    }

    // It removes several Blocks, and all its properties related and stored separately;
    private void _removeBlocks(Transaction tr, List<String> blockHashes) {
        blockHashes.forEach(h -> {
            _removeBlock(tr, h);
            _unlinkBlock(h);
        });
    }

    // retrieves the Block Header in serialized format
    private byte[] _getBlockBytes(Transaction tr, String blockHash) {
        return read(tr, key(blocksDir, keyForBlock(blockHash)));
    }

    // It retrieves a Block Header:
    private BlockHeader _getBlock(Transaction tr, String blockHash) {
       // We recover the Whole Block Header:
       BlockHeader result = toBlockHeader(_getBlockBytes(tr, blockHash));
       if (result == null ) return null;

       // We recover the "numTxs" property and feed it with that:
       Long numTxs = toLong(read(tr, key(blocksDir, keyForBlockNumTxs(blockHash))));
       result = result.toBuilder().numTxs(numTxs).build();

       return result;
    }

    // It saves a Tx into the DB: the whole Tx, and its individual properties
    private void _saveTx(Transaction tr, Tx tx) {
        // We store the Whole TX Object
        save(tr, key(txsDir, keyForTx(tx.getHash().toString())), bytes(tx));

        // We store the list of Tx Hashes this TX depends on:
        Set<String> txsNeeded = tx.getInputs().stream()
                .map(i -> i.getOutpoint().getHash().toString())
                .collect(Collectors.toSet());
        HashesList txsHashesNeeded = HashesList.builder().hashes(new ArrayList<>(txsNeeded)).build();
        save(tr, key(txsDir, keyForTxsNeeded(tx.getHash().toString())), bytes(txsHashesNeeded));
    }

    // It saves several Txs into the DB
    private void _saveTxs(Transaction tr, List<Tx> txs) {
        txs.forEach(tx -> _saveTx(tr, tx));
    }

    // It returns the TX in raw format (byte array, not deserialized yet)
    private byte[] _getTxBytes(Transaction tr, String txHash) {
        return read(tr, key(txsDir, keyForTx(txHash)));
    }

    // It returns the TX from the DB
    private Tx _getTx(Transaction tr, String txHash) {
        return toTx(_getTxBytes(tr, txHash));
    }

    // It Returns the List of Hashes that represents the Blocks the TX given belongs to (might be more than one)
    private HashesList _getBlockHashesLinkedToTx(Transaction tr, String txHash) {
        HashesList result = toHashes(read(tr, key(txsDir,keyForTxBlocks(txHash))));
        return (result != null) ? result : HashesList.builder().build();
    }

    // It returns the List of Hashes that represents the Txs the Tx given depends on
    private HashesList _getTxHashesNeededByTx(Transaction tr, String txHash) {
        HashesList result = toHashes(read(tr, key(txsDir, keyForTxsNeeded(txHash))));
        return (result != null) ? result : HashesList.builder().build();
    }

    // I removes a whole TX and the individual properties stored for it:
    private void _removeTx(Transaction tr, String txHash) {
        remove(tr, key(txsDir, keyForTx(txHash)));
        remove(tr, key(txsDir, keyForTxBlocks(txHash)));
        remove(tr, key(txsDir, keyForTxsNeeded(txHash)));
    }

    // I removes several TXs and the individual properties stored for them:
    private void _removeTxs(Transaction tr, List<String> txHashes) {
        txHashes.forEach(h -> _removeTx(tr, h));
    }

    // It links the Tx wth the block given:
    private void _linkTxToBlock(Transaction tr, String txHash, String blockHash) {
        // We locate the Block Subfolder:
        DirectorySubspace blockDir = openDir(tr, blocksDir, keyForBlockDir(blockHash));
        _linkTxToBlock(tr, txHash, blockHash, blockDir);
    }

    // It links the Tx wth the block given, specifying also the Block Directory:
    private void _linkTxToBlock(Transaction tr, String txHash, String blockHash, DirectorySubspace blockDir) {
        // We add a Key in this Block subfolder for this Tx:
        save(tr, key(blockDir, keyForBlockTx(txHash)), bytes(1L)); // the value is NOT important here...

        // We get the "blocks" property of this Tx and add a reference to this Block:
        HashesList txBlockHashes = _getBlockHashesLinkedToTx(tr, txHash);
        txBlockHashes.addHash(blockHash);
        save(tr, key(txsDir, keyForTxBlocks(txHash)), bytes(txBlockHashes));
    }


    // It removes the relationship between the Tx and the Block given:
    private void _unlinkTxFromBlock(Transaction tr, String txHash, String blockHash) {
        // We locate the Block Subfolder:
        DirectorySubspace blockDir = openDir(tr, blocksDir, keyForBlockDir(blockHash));
        _unlinkTxFromBlock(tr, txHash, blockHash, blockDir);
    }

    // It removes the relationship between the Tx and the Block given, specying also the Block Dir:
    private void _unlinkTxFromBlock(Transaction tr, String txHash, String blockHash, DirectorySubspace blockDir) {
        // We remove a Key from ths Block subfolder:
        remove(tr, key(blockDir, keyForBlockTx(txHash)));

        // We remove the reference to this Block from this Tx:
        HashesList txBlockHashes = _getBlockHashesLinkedToTx(tr, txHash);
        if (txBlockHashes.getHashes().contains(blockHash)) {
            txBlockHashes.removeHash(blockHash);
            save(tr, key(txsDir, keyForTxBlocks(txHash)), bytes(txBlockHashes));
        }
    }

    // It unlinks this Tx from any block it might be related to
    private void _unlinkTx(Transaction tr, String txHash) {
        HashesList blocksLinked = _getBlockHashesLinkedToTx(tr, txHash);
        blocksLinked.getHashes().forEach(blockHash -> {
            DirectorySubspace blockDir = openDir(tr, blocksDir, keyForBlockDir(blockHash));
            remove(tr, key(blockDir, keyForBlockTx(txHash)));
        });

        // We update the "blocks" property of this Tx:
        blocksLinked.clear();
        save(tr, key(txsDir, keyForTxBlocks(txHash)), bytes(blocksLinked));
    }

    // It unlinks a whole blocks from all its Txs.
    private void _unlinkBlock(String blockHash) {

        // We locate the Bock Subfolder and the Key to start iterating over:
        DirectorySubspace blockDir = _openDir(blocksDir, keyForBlockDir(blockHash));
        byte[] keyStart = keyComparisonPreffix(key(blockDir, KeyValueUtils.KEY_PREFFIX_TX_LINK));

        // For each Tx in this Block, we update its "blocks" property, removing the reference to this Block. In order
        // to avoid breaking the FoundationDB limitations, we iterate over the Tx in a "safe" way: We create a
        // Transaction to use in the iteration, and we close it and create another one before reaching the Transaction
        // limit. All of thi is carried out by the "loopOverKeys" method that accepts a Database as a first parameter.

        loopOverKeysAndRun(this.db, keyStart, (transaction, key) -> {
            String txHash = extractTxHashFromKey(key).get();
            HashesList blocksLinkedToTx = _getBlockHashesLinkedToTx(transaction, txHash);
            if (!blocksLinkedToTx.getHashes().isEmpty()) {
                blocksLinkedToTx.removeHash(blockHash);
                save(transaction, key(txsDir, keyForTxBlocks(txHash)), bytes(blocksLinkedToTx));
            }
        });

        // Now we remove the whole Block directory:
        db.run(tr -> {
            blocksDir.remove(tr, Arrays.asList(keyForBlockDir(blockHash))).join();
            return null;
        });
    }

    private boolean _isTxLinkToBlock(Transaction tr, String txHash, String blockHash) {
        HashesList blocksLinked = _getBlockHashesLinkedToTx(tr, txHash);
        return blocksLinked.getHashes().contains(blockHash);
    }

    // It removes from the DB all the TX linked/belonging to the Block given.

    private void _removeBlockTxs(String blockHash, Consumer<String> txHashConsumer) {

        // We locate the Bock Subfolder and the Key to start iterating over:
        DirectorySubspace blockDir = _openDir(blocksDir, keyForBlockDir(blockHash));
        byte[] keyStart = keyComparisonPreffix(key(blockDir, KeyValueUtils.KEY_PREFFIX_TX_LINK));

        // For each Tx in this Block, we update its "blocks" property, removing the reference to this Block. In order
        // to avoid breaking the FoundationDB limitations, we iterate over the Tx in a "safe" way: We create a
        // Transaction to use in the iteration, and we close it and create another one before reaching the Transaction
        // limit. All of thi is carried out by the "loopOverKeys" method that accepts a Database as a first parameter.

        loopOverKeysAndRun(this.db, keyStart, (transaction, key) -> {
            String txHash = extractTxHashFromKey(key).get();
            _removeTx(transaction, txHash);
            txHashConsumer.accept(txHash);
        });

        // Now we remove the whole Block directory:
        db.run(tr -> {
            blocksDir.remove(tr, Arrays.asList(keyForBlockDir(blockHash))).join();
            return null;
        });
    }


    // It triggers a BLOCKS_STORED Event, if enabled
    private void _triggerBlocksStoredEvent(List<BlockHeader> blockHeaders) {
        if (triggerBlockEvents) {
            List<Sha256Wrapper> blockHashes = blockHeaders.stream().map(b -> b.getHash()).collect(Collectors.toList());
            eventBus.publish(BlocksSavedEvent.builder().blockHashes(blockHashes).build());
        }
    }

    // It triggers a BLOCKS_REMOVED Event, if enabled
    private void _triggerBlocksRemovedEvent(List<Sha256Wrapper> blockHashes) {
        if (triggerBlockEvents)
            eventBus.publish(BlocksRemovedEvent.builder().blockHashes(blockHashes).build());
    }

    // It triggers a TXS_STORED Event, if enabled
    private void _triggerTxsStoredEvent(List<Tx> txs) {
        if (triggerTxEvents) {
            List<Sha256Wrapper> txHashes = txs.stream().map(tx -> tx.getHash()).collect(Collectors.toList());
            eventBus.publish(TxsSavedEvent.builder().txHashes(txHashes).build());
        }
    }

    // It triggers a TXS_REMOVED Event, if enabled
    private void _triggerTxsRemovedEvent(List<Sha256Wrapper> txHashes) {
        if (triggerTxEvents)
            eventBus.publish(TxsRemovedEvent.builder().txHashes(txHashes).build());
    }

    /**
     * This methods initializes the connection to the DB and it initialises the internal Directory structure of the Data.
     * If no cluster File is specified in the Configuration, then the default location is used (check documentation for
     * the default cluster file location depending on the OS):
     * <br />
     * <a href="https://apple.github.io/foundationdb/administration.html" >Foundation DB Administration</a>
     */
    @Override
    public void start() {
        // We init the DB Connection...
        fdb = FDB.selectAPIVersion(config.getApiVersion());
        db = (config.getClusterFile() != null)? fdb.open() : fdb.open(config.getClusterFile());

        // We initialize the Directory Layer and the directory structure:
        dirLayer = new DirectoryLayer();
        db.run( tr -> {

           blockchainDir    = dirLayer.createOrOpen(tr, Arrays.asList(KeyValueUtils.DIR_BLOCKCHAIN)).join();
           netDir           = blockchainDir.createOrOpen(tr, Arrays.asList(config.getNetworkId())).join();
           blocksDir        = netDir.createOrOpen(tr, Arrays.asList(KeyValueUtils.DIR_BLOCKS)).join();
           txsDir           = netDir.createOrOpen(tr, Arrays.asList(KeyValueUtils.DIR_TXS)).join();

           // We print out the DB Structure and general info about the configuration:
           log.info("JCL-Store Configuration:");
           log.info(" - FoundationDB Implementation");
           log.info(" - Network : " + config.getNetworkId());

           return null;
        });

    }

    @Override
    public void stop() {
        this.db.close();
    }

    @Override
    public void saveBlock(BlockHeader blockHeader) {
        db.run(tr -> {
            _saveBlock(tr, blockHeader);
            _triggerBlocksStoredEvent(Arrays.asList(blockHeader));
            return null;
        });
    }

    @Override
    public void saveBlocks(List<BlockHeader> blockHeaders) {
        /*
            In order to workaround the FoundationDB limitations, we break down the list of Items in smaller lists,
            and we create one Transaction per each subLit.
            https://apple.github.io/foundationdb/known-limitations.html
         */
        List<List<BlockHeader>> subLists = Lists.partition(blockHeaders, config.getTransactionBatchSize());
        for (List<BlockHeader> subList : subLists) {
            db.run(tr -> {
                _saveBlocks(tr, subList);
                return null;
            });
        }
        _triggerBlocksStoredEvent(blockHeaders);
    }

    @Override
    public boolean containsBlock(Sha256Wrapper blockHash) {
        AtomicBoolean result = new AtomicBoolean();
        db.run(tr -> {
            result.set(_getBlockBytes(tr, blockHash.toString()) != null);
            return null;
        });
        return result.get();
    }

    @Override
    public Optional<BlockHeader> getBlock(Sha256Wrapper blockHash) {
        AtomicReference<BlockHeader> result = new AtomicReference<>();
        db.run(tr -> {
            result.set(_getBlock(tr, blockHash.toString()));
            return null;
        });
        return Optional.ofNullable(result.get());
    }

    @Override
    public void removeBlock(Sha256Wrapper blockHash) {
        db.run(tr -> {
            _removeBlock(tr, blockHash.toString());
            _unlinkBlock(blockHash.toString());
            _triggerBlocksRemovedEvent(Arrays.asList(blockHash));
            return null;
        });
    }

    @Override
    public void removeBlocks(List<Sha256Wrapper> blockHashes) {
        /*
            In order to workaround the FoundationDB limitations, we break down the list of Items in smaller lists,
            and we create one Transaction per each subLit.
            https://apple.github.io/foundationdb/known-limitations.html
         */
        List<List<Sha256Wrapper>> subLists = Lists.partition(blockHashes, config.getTransactionBatchSize());
        for (List<Sha256Wrapper> subList : subLists) {
            db.run(tr -> {
               _removeBlocks(tr, subList.stream().map(h -> h.toString()).collect(Collectors.toList()));
                return null;
            });
        }
        _triggerBlocksRemovedEvent(blockHashes);
    }

    @Override
    public long getNumBlocks() {
        long result = numKeys(this.db, blocksDir, KeyValueUtils.KEY_PREFFIX_BLOCK.getBytes());
        return result;
    }

    @Override
    public void saveTx(Tx tx) {
        db.run(tr -> {
            _saveTx(tr, tx);
            _triggerTxsStoredEvent(Arrays.asList(tx));
            return null;
        });
    }

    @Override
    public void saveTxs(List<Tx> txs) {
        /*
            In order to workaround the FoundationDB limitations, we break down the list of Items in smaller lists,
            and we create one Transaction per each subLit.
            https://apple.github.io/foundationdb/known-limitations.html
         */
        List<List<Tx>> subLists = Lists.partition(txs, config.getTransactionBatchSize());
        for (List<Tx> subList : subLists) {
            db.run(tr -> {
                _saveTxs(tr, subList);
                return null;
            });
        }
        _triggerTxsStoredEvent(txs);
    }

    @Override
    public boolean containsTx(Sha256Wrapper txHash) {
        AtomicBoolean result = new AtomicBoolean();
        db.run(tr -> {
            result.set(_getTxBytes(tr, txHash.toString()) != null);
            return null;
        });
        return result.get();
    }

    @Override
    public Optional<Tx> getTx(Sha256Wrapper txHash) {
        AtomicReference<Tx> result = new AtomicReference<>();
        db.run(tr -> {
            result.set(_getTx(tr, txHash.toString()));
            return null;
        });
        return Optional.ofNullable(result.get());
    }

    @Override
    public void removeTx(Sha256Wrapper txHash) {
        db.run(tr -> {
            _removeTx(tr, txHash.toString());
            _triggerTxsRemovedEvent(Arrays.asList(txHash));
            return null;
        });
    }

    @Override
    public void removeTxs(List<Sha256Wrapper> txHashes) {
        /*
            In order to workaround the FoundationDB limitations, we break down the list of Items in smaller lists,
            and we create one Transaction per each subLit.
            https://apple.github.io/foundationdb/known-limitations.html
         */
        List<List<Sha256Wrapper>> subLists = Lists.partition(txHashes, config.getTransactionBatchSize());
        for (List<Sha256Wrapper> subList : subLists) {
            db.run(tr -> {
                _removeTxs(tr, subList.stream().map(h -> h.toString()).collect(Collectors.toList()));
                return null;
            });
        }
        _triggerTxsRemovedEvent(txHashes);
    }

    @Override
    public List<Sha256Wrapper> getTxsNeeded(Sha256Wrapper txHash) {
        List<Sha256Wrapper> result = new ArrayList<>();
        db.run(tr -> {
            HashesList hashes = _getTxHashesNeededByTx(tr, txHash.toString());
            result.addAll(hashes.getHashes().stream()
                    .map(h -> Sha256Wrapper.wrap(h))
                    .collect(Collectors.toList()));
           return null;
        });
        return result;
    }

    @Override
    public long getNumTxs() {
        long result = numKeys(this.db, txsDir, KeyValueUtils.KEY_PREFFIX_TX.getBytes());
        return result;
    }

    @Override
    public void linkTxToBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        db.run(tr -> {
           _linkTxToBlock(tr, txHash.toString(), blockHash.toString());
           return null;
        });
    }

    @Override
    public void unlinkTxFromBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        db.run(tr -> {
            _unlinkTxFromBlock(tr, txHash.toString(), blockHash.toString());
           return null;
        });
    }

    @Override
    public void unlinkTx(Sha256Wrapper txHash) {
        db.run(tr -> {
           _unlinkTx(tr, txHash.toString());
           return null;
        });
    }

    @Override
    public void unlinkBlock(Sha256Wrapper blockHash) {
        _unlinkBlock(blockHash.toString());

    }

    @Override
    public boolean isTxLinkToblock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        AtomicBoolean result = new AtomicBoolean();
        db.run(tr -> {
            result.set(_isTxLinkToBlock(tr, txHash.toString(), blockHash.toString()));
            return null;
        });
        return result.get();
    }

    @Override
    public List<Sha256Wrapper> getBlockHashLinkedToTx(Sha256Wrapper txHash) {
        List<Sha256Wrapper> result = new ArrayList<>();
        db.run(tr -> {
            HashesList hashes = _getBlockHashesLinkedToTx(tr, txHash.toString());
            if (hashes != null && hashes.getHashes() != null) {
                result.addAll(hashes.getHashes().stream().map(h -> Sha256Wrapper.wrap(h)).collect(Collectors.toList()));
            }
            return null;
        });
        return result;
    }

    @Override
    public Iterable<Sha256Wrapper> getBlockTxs(Sha256Wrapper blockHash) {
        if (!containsBlock(blockHash)) return null; // Check

        DirectorySubspace blockDir = _openDir(blocksDir, blockHash.toString());
        Iterator<Sha256Wrapper> it = FDBSafeIterator.<Sha256Wrapper>longIteratorBuilder()
                                        .database(this.db)
                                        .fromDir(blockDir)
                                        .startingWithPreffix(bytes(KEY_PREFFIX_TX_LINK))
                                        .buildItemBy(kv -> Sha256Wrapper.wrap(extractTxHashFromKey(kv.getKey()).get()))
                                        .build();
        Iterable<Sha256Wrapper> result = () -> it;
        return result;
    }

    @Override
    public long getBlockNumTxs(Sha256Wrapper blockHash) {
        DirectorySubspace blockDir = _openDir(blocksDir, blockHash.toString());
        long result =  numKeys(this.db, blockDir, KEY_PREFFIX_TX_LINK.getBytes());
        return result;
    }

    @Override
    public void saveBlockTxs(Sha256Wrapper blockHash, List<Tx> txs) {
        /*
            In order to workaround the FoundationDB limitations, we break down the list of Items in smaller lists,
            and we create one Transaction per each subLit.
            https://apple.github.io/foundationdb/known-limitations.html
         */
        List<List<Tx>> subLists = Lists.partition(txs, config.getTransactionBatchSize());

        DirectorySubspace blockDir = _openDir(blocksDir, keyForBlockDir(blockHash.toString()));
        AtomicInteger countTxs = new AtomicInteger();
        for (List<Tx> sublist : subLists) {
            db.run(tr -> {
                sublist.forEach(tx -> {
                    _saveTx(tr, tx);
                    _linkTxToBlock(tr, tx.getHash().toString(), blockHash.toString(), blockDir);
                });
                countTxs.addAndGet(sublist.size());
                return null;
            });
        } // for...
        _triggerTxsStoredEvent(txs);
    }

    @Override
    public void removeBlockTxs(Sha256Wrapper blockHash) {

        // We remove all The Txs from this Block. the problem is that we need to trigger an TXS_REMOVED Event but the
        // number of Txs must be huge, so we cannot just trigger a single event with a huge list inside. Instead, we
        // are keeping track of the number of Txs we remove, and we only trigger an event when we reach the Threshold.
        // We put all that logic inside a Lambda function that will be passed to the method that removes the Txs...

        List<Sha256Wrapper> batchTxsRemoved = new ArrayList<>();
        Consumer<String> txHashConsumer = txHash -> {
            batchTxsRemoved.add(Sha256Wrapper.wrap(txHash));
            if (batchTxsRemoved.size() == MAX_EVENT_ITEMS) {
                _triggerTxsRemovedEvent(batchTxsRemoved);
                batchTxsRemoved.clear();
            }
        };

        db.run(tr -> {
            _removeBlockTxs(blockHash.toString(), txHashConsumer);
            return null;
        });

        // In case there are still Tx that have not been published in an Event...
        if (batchTxsRemoved.size() > 0) _triggerTxsRemovedEvent(batchTxsRemoved);
    }

    @Override
    public Optional<BlocksCompareResult> compareBlocks(Sha256Wrapper blockHashA, Sha256Wrapper blockHashB) {
        Optional<BlockHeader> blockHeaderA = getBlock(blockHashA);
        Optional<BlockHeader> blockHeaderB = getBlock(blockHashB);

        if (blockHeaderA.isEmpty() || blockHeaderB.isEmpty()) return Optional.empty();

        BlocksCompareResult.BlocksCompareResultBuilder resultBuilder = BlocksCompareResult.builder()
                .blockA(blockHeaderA.get())
                .blockB(blockHeaderB.get());

        DirectorySubspace dirA = _openDir(blocksDir, blockHashA.toString());
        DirectorySubspace dirB = _openDir(blocksDir, blockHashB.toString());

        // We create an Iterable for the TXs in common:

        BiPredicate<Transaction, byte[]> commonKeyValid = (tr, k) -> _isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashB.toString());
        Iterator<Sha256Wrapper> commonIterator = FDBSafeIterator.<Sha256Wrapper>longIteratorBuilder()
                .database(this.db)
                .fromDir(dirA)
                .keyIsValidWhen(commonKeyValid)
                .buildItemBy(k -> Sha256Wrapper.wrap(extractTxHashFromKey(k.getKey()).get()))
                .build();
        Iterable<Sha256Wrapper> commonIterable = () -> commonIterator;
        resultBuilder.txsInCommonIt(commonIterable);

        // We create an Iterable for the TXs that are ONLY in the block A:

        BiPredicate<Transaction, byte[]> onlyAKeyValid = (tr, k) -> !_isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashB.toString());
        Iterator<Sha256Wrapper> onlyAIterator = FDBSafeIterator.<Sha256Wrapper>longIteratorBuilder()
                .database(this.db)
                .fromDir(dirA)
                .keyIsValidWhen(onlyAKeyValid)
                .buildItemBy(k -> Sha256Wrapper.wrap(extractTxHashFromKey(k.getKey()).get()))
                .build();
        Iterable<Sha256Wrapper> onlyAIterable = () -> onlyAIterator;
        resultBuilder.txsOnlyInA(onlyAIterable);

        // We create an Iterable for the TXs that are ONLY in the block B:
        BiPredicate<Transaction, byte[]> onlyBKeyValid = (tr, k) -> !_isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashA.toString());
        Iterator<Sha256Wrapper> onlyBIterator = FDBSafeIterator.<Sha256Wrapper>longIteratorBuilder()
                .database(this.db)
                .fromDir(dirB)
                .keyIsValidWhen(onlyBKeyValid)
                .buildItemBy(k -> Sha256Wrapper.wrap(extractTxHashFromKey(k.getKey()).get()))
                .build();
        Iterable<Sha256Wrapper> onlyBIterable = () -> onlyBIterator;
        resultBuilder.txsOnlyInB(onlyBIterable);


        return Optional.of(resultBuilder.build());
    }

    @Override
    public BlockStoreStreamer EVENTS() {
        return this.blockStoreStreamer;
    }

    // ONLY FOR TESTING
    @Override
    public long getNumKeys(String keyPrefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void printKeys() {
        log.debug("Directoy layout:");
        db.run(tr -> {
            KeyValueUtils.printDir(tr, blockchainDir, 0, true);
            return null;
        });
    }
}
