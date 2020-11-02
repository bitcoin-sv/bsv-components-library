package com.nchain.jcl.store.levelDB.blockStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.serialization.BlockHeaderSerializer;
import com.nchain.jcl.base.serialization.TxSerializer;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockStore.BlockStore;
import com.nchain.jcl.store.blockStore.events.*;
import com.nchain.jcl.store.common.PaginatedRequest;
import com.nchain.jcl.store.common.PaginatedResult;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of the BlockStore component using LevelDB as the Tech Stack.
 *
 * LevelDB is a No-SQL that provides Key-Value storage. This implementation stores the information using the
 * following keys:
 *
 *
 *  - key: b:[block_hash] (example: "b:00a12918c6674caf3c96ce977372eb0bf41cbca687683251f199cdc1d988c7b6")
 *  - value: a BlockHeader.
 *
 *  - key: tx:[tx_hash] (example: "tx:187b49bcae37859e34e1c695d842473f1f65db9ef2b29854eabb168e1663e068")
 *  - value: a Transaction.
 *
 *  - key: btx:[block_hash]:[tx_hash] (example: "btx:00a12918c6674caf3c...:187b49bcae37859e34e1c695d842473...")
 *  - value_ Just a 1-bte array. The value is not important here, only the Key. If this key exists, then the Block
 *    identified by block_hash contains the Tx identified by tx_hash.
 *
 *  - key: txb:[tx_hash] (example: "txb:187b49bcae37859e34e1c695d842473f1f65db9ef2b29854eabb168e1663e068")
 *  - value: The Block Hash this Tx is lined to.
 *
 */

@Slf4j
public class BlockStoreLevelDB implements BlockStore {

    // Prefixes used to generate the Keys:
    private static final String PREFFIX_KEY_BLOCK       = "b:";
    private static final String PREFFIX_KEY_TX          = "tx:";
    private static final String PREFFIX_KEY_BLOCKTXS    = "btx:";
    private static final String PREFFIX_KEY_TXBLOCK     = "txb:";

    // When triggering Events, some times a Pagination is needed:
    private static final int TX_PAG_SIZE_DEFAULT = 10_000;

    // Working Folder where te LevelDB files will be stored:
    private static final String LEVELDB_FOLDER = "levelDB-blockStore";

    // Configuration:
    private final BlockStoreLevelDBConfig config;
    private final boolean triggerBlockEvents;
    private final boolean triggerTxEvents;

    // Events Configuration:
    private final ExecutorService executorService;
    protected final EventBus eventBus;
    private final BlockStoreStreamer blockStoreStreamer;

    // LevelDB instance:
    protected final DB levelDBStore;

    // We keep an internal reference here of the Serializers we'll need:
    private static final BlockHeaderSerializer headerSer = BlockHeaderSerializer.getInstance();
    private static final TxSerializer txSer = TxSerializer.getInstance();

    /** Constructor */
    @Builder
    public BlockStoreLevelDB(BlockStoreLevelDBConfig config,
                             Boolean triggerBlockEvents,
                             Boolean triggerTxEvents) throws RuntimeException {
        try {
            this.config = config;
            this.triggerBlockEvents = (triggerBlockEvents != null) ? triggerBlockEvents : false;
            this.triggerTxEvents = (triggerTxEvents != null) ? triggerTxEvents : false;

            // LevelDB engine configuration:
            Options options = new Options();
            Path levelDBPath = Paths.get(config.getWorkingFolder().toString(), LEVELDB_FOLDER);
            levelDBStore = factory.open(levelDBPath.toFile(), options);

            // Events Configuration:
            this.executorService = ThreadUtils.getThreadPoolExecutorService("BlockStore-LevelDB");
            this.eventBus = EventBus.builder().executor(this.executorService).build();
            this.blockStoreStreamer = new BlockStoreStreamer(this.eventBus);

        } catch (IOException ioe) {
            log.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    // Functions to generate Keys in String format:

    protected String getKeyForBlock(String blockHash) { return PREFFIX_KEY_BLOCK + blockHash; }
    protected String getKeyForTx(String txHash) { return PREFFIX_KEY_TX + txHash;}
    protected String getKeyForBlockTx(String blockHash, String txHash) { return PREFFIX_KEY_BLOCKTXS + blockHash + ":" + txHash; }
    protected String getKeyForTxBlock(String txHash) { return PREFFIX_KEY_TXBLOCK + txHash; }

    // Functions to extract some Hashes from the Keys:

    private String getBlockHashFromKey(String key) {
        if (key.startsWith(PREFFIX_KEY_BLOCK)) return key.substring(key.indexOf(":") + 1);
        if (key.startsWith(PREFFIX_KEY_BLOCKTXS)) return key.substring(key.indexOf(":") + 1, key.lastIndexOf(":"));
        return null;
    }

    private String getTxHashFromKey(String key) {
        if (key.startsWith(PREFFIX_KEY_TX)) return key.substring(key.indexOf(":") + 1);
        if (key.startsWith(PREFFIX_KEY_BLOCKTXS)) return key.substring(key.lastIndexOf(":") + 1);
        if (key.startsWith(PREFFIX_KEY_TXBLOCK)) return key.substring(key.indexOf(":") + 1);
        return null;
    }

    // Functions to convert keys/values to byte arrays:
    // for those Beans in JCL-Base, we are using the Serializers from that package instead of using a default
    // Java-Serializer, since they are expected to be more efficient.

    private byte[]   bytes(BlockHeader header) { return headerSer.serialize(header); }
    private byte[]   bytes(Tx tx) { return txSer.serialize(tx);}
    protected byte[] bytes(String value) { return value.getBytes(); }

    // Functions to convert byte[] to values:

    protected boolean   isBytesOk(byte[] bytes) { return (bytes != null && bytes.length > 0);}
    private BlockHeader bytesToBlockHeader(byte[] bytes) { return (isBytesOk(bytes)) ? headerSer.deserialize(bytes) : null;}
    private Tx          bytesToTx(byte[] bytes) { return (isBytesOk(bytes)) ? txSer.deserialize(bytes) : null; }

    /**
     * A generic method to loop over Keys and to perform an operation with each one of them.
     * It allows for pagination by using the "startingKeyIndex" and "maxKeysToProcess".
     *
     * @param keyPrefix         Key prefix. Only those Keys that start with these Preffix will be processed
     * @param startingKeyIndex  The first Key found has the index 0, the next 1, and so on. Only the Keys which index
     *                          is equals or higher than "startingKeyIndex" will be processed.
     * @param maxKeysToProcess  Maximum number of Keys to process. If not presents, all the keys found are processed.
     * @param taskForKey        A piece of code that is executed for each Key that is found.
     * @param taskForAllKeys    A piece of code that is executed for all the Keys retrieved
     *                          IMPORTANT: The task defined hare is a consumer that takes aList of Keys (String) as
     *                          a parameter. So we need to consider how many Keys are we going to loop over, if the
     *                          number of them is too big, this coud cause a memory problem when passing a list of
     *                          all those keys to this task. So only use this task when you are confident that the
     *                          number of keys won't break the memory.
     */
    private void _loopOverKeysAndRun(String keyPrefix,
                                     Long startingKeyIndex,
                                     Optional<Long> maxKeysToProcess,
                                     Consumer<String> taskForKey,
                                     Consumer<List<String>> taskForAllKeys) {
        try {
            DBIterator iterator = levelDBStore.iterator();
            iterator.seek(bytes(keyPrefix));

            // We store each one of the Keys we process, so we can also trigger the global "taskForAllKeys", passing
            // all of them as a parameter. This list could be potentially huge, so we only use it if the
            // "taskForAllKeys" as been set (not null)
            List<String> allKeysToProcess = new ArrayList<>();

            long currentIndex = -1;
            long numKeysProcessed = 0;
            while(true) {
                if (!iterator.hasNext()) break;

                // We advance the Iterator...
                String key = new String(iterator.next().getKey());
                currentIndex++;

                // Exit Conditions:
                if (!key.startsWith(keyPrefix)) break;
                if (maxKeysToProcess.isPresent() && (numKeysProcessed >= maxKeysToProcess.get())) break;
                if (currentIndex < startingKeyIndex) continue;

                // If we reach this far then it means that we have to process it, so we do it and move forward:
                taskForKey.accept(key);
                numKeysProcessed++;

                // We only use this list if the global task has been defined:
                if (taskForAllKeys != null) allKeysToProcess.add(key);
            }
            iterator.close();

            // We are done iterating over the Keys. We trigger the global Task:
            if (taskForAllKeys != null) taskForAllKeys.accept(allKeysToProcess);

        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            throw new RuntimeException(ioe);
        }
    }

    // Convenience method without pagination
    private void _loopOverKeysAndRun(String keyPrefix, Consumer<String> individualTask, Consumer<List<String>> globalTask) {
        _loopOverKeysAndRun(keyPrefix, 0L, Optional.empty(), individualTask, globalTask);
    }

    // Basic operations:
    // This operations make the changes in the DB but do NOT trigger Events.
    // They work with more basic data types like String, which are closer to the Types used for the Keys in LevelDB.
    // Each function is responsible for leaving the DB in a consistent state. For example, the method that removes
    // a TX will also take care of removing the relationships between that Tx an the Block its linked to, if any.

    protected void _saveBlock(BlockHeader blockHeader) {
        String key = getKeyForBlock(blockHeader.getHash().toString());
        levelDBStore.put(bytes(key), bytes(blockHeader));
    }

    protected void _removeBlock(String blockHashHex) {
        String key = getKeyForBlock(blockHashHex);
        levelDBStore.delete(bytes(key));
        _unlinkBlock(blockHashHex);
    }

    private void _saveTx(Tx tx) {
        String key = getKeyForTx(tx.getHash().toString());
        levelDBStore.put(bytes(key), bytes(tx));
    }

    private void _removeTx(String txHash) {
        String key = getKeyForTx(txHash);
        levelDBStore.delete(bytes(key));
        _unlinkTx(txHash);
    }

    private Optional<String> _getBlockHashLinkedToTx(String txHashHex) {
        String key = PREFFIX_KEY_TXBLOCK + txHashHex;
        byte[] value = levelDBStore.get(bytes(key));
        Optional<String> result = (value != null) ? Optional.of(new String(value)) : Optional.empty();
        return result;
    }

    private void _linkTxToBlock(String txHashHex, String blockHashHex) {
        String bTxKey = getKeyForBlockTx(blockHashHex, txHashHex);
        String txBKey = getKeyForTxBlock(txHashHex);
        levelDBStore.put(bytes(bTxKey), new byte[]{1});
        levelDBStore.put(bytes(txBKey), bytes(blockHashHex));
    }

    private void _unlinkTxFromBlock(String txHashHex, String blockHashHex) {
        String bTxKey = getKeyForBlockTx(blockHashHex, txHashHex);
        String txBKey = getKeyForTxBlock(txHashHex);
        levelDBStore.delete(bytes(bTxKey));
        levelDBStore.delete(bytes(txBKey));
    }

    private void _unlinkTx(String txHashHex) {
        _getBlockHashLinkedToTx(txHashHex).ifPresent(blockHashHex -> _unlinkTxFromBlock(txHashHex, blockHashHex));
    }

    private void _unlinkBlock(String blockHashHex) {
        // we loop over the "btx" keys, and for each one we remove it and also remove the "txb" entry:
        String keyPrefix = PREFFIX_KEY_BLOCKTXS + blockHashHex;
        _loopOverKeysAndRun(keyPrefix, k -> _unlinkTxFromBlock(getTxHashFromKey(k), blockHashHex), null);
    }

    private void _triggerBlocksStoredEvent(List<BlockHeader> blockHeaders) {
        if (triggerBlockEvents) {
            List<Sha256Wrapper> blockHashes = blockHeaders.stream().map(b -> b.getHash()).collect(Collectors.toList());
            eventBus.publish(BlocksSavedEvent.builder().blockHashes(blockHashes).build());
        }
    }

    private void _triggerBlocksRemovedEvent(List<Sha256Wrapper> blockHashes) {
        if (triggerBlockEvents)
            eventBus.publish(BlocksRemovedEvent.builder().blockHashes(blockHashes).build());
    }

    private void _triggerTxsStoredEvent(List<Tx> txs) {
        if (triggerTxEvents) {
            List<Sha256Wrapper> txHashes = txs.stream().map(tx -> tx.getHash()).collect(Collectors.toList());
            eventBus.publish(TxsSavedEvent.builder().txHashes(txHashes).build());
        }
    }

    private void _triggerTxsRemovedEvent(List<Sha256Wrapper> txHashes) {
        if (triggerTxEvents)
            eventBus.publish(TxsRemovedEvent.builder().txHashes(txHashes).build());
    }

    // Init/post operations:

    @Override
    public void start() {
        log.debug("Level DB Starting..");
    }

    @Override
    public void stop() {
        try {
            levelDBStore.close();
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            throw new RuntimeException(ioe);
        }
    }

    // Block Storage Operations:

    @Override
    public void saveBlock(BlockHeader blockHeader) {
        _saveBlock(blockHeader);
        _triggerBlocksStoredEvent(Arrays.asList(blockHeader));
    }

    @Override
    public void saveBlocks(List<BlockHeader> blockHeaders) {
        blockHeaders.forEach(b -> _saveBlock(b));
        _triggerBlocksStoredEvent(blockHeaders);
    }

    @Override
    public boolean containsBlock(Sha256Wrapper blockHash) {
        return levelDBStore.get(bytes(getKeyForBlock(blockHash.toString()))) != null;
    }

    @Override
    public Optional<BlockHeader> getBlock(Sha256Wrapper blockHash) {
        byte[] value = levelDBStore.get(bytes(getKeyForBlock(blockHash.toString())));
        Optional<BlockHeader> result = Optional.ofNullable(bytesToBlockHeader(value));
        return result;
    }

    @Override
    public void removeBlock(Sha256Wrapper blockHash) {
        _removeBlock(blockHash.toString());
        _triggerBlocksRemovedEvent(Arrays.asList(blockHash));
    }

    @Override
    public void removeBlocks(List<Sha256Wrapper> blockHashes) {
        blockHashes.forEach(b -> _removeBlock(b.toString()));
        _triggerBlocksRemovedEvent(blockHashes);
    }

    @Override
    public long getNumBlocks() {
        AtomicLong result = new AtomicLong();
        _loopOverKeysAndRun(PREFFIX_KEY_BLOCK, k -> result.incrementAndGet(), null);
        return result.get();
    }

    // Tx Storage Operations:

    @Override
    public void saveTx(Tx tx) {
        _saveTx(tx);
        _triggerTxsStoredEvent(Arrays.asList(tx));
    }

    @Override
    public void saveTxs(List<Tx> txs) {
        txs.forEach( h -> _saveTx(h));
        _triggerTxsStoredEvent(txs);
    }

    @Override
    public boolean containsTx(Sha256Wrapper txHash) {
        return levelDBStore.get(bytes(getKeyForTx(txHash.toString()))) != null;
    }

    @Override
    public Optional<Tx> getTx(Sha256Wrapper txHashHex) {
        byte[] value = levelDBStore.get(bytes(getKeyForTx(txHashHex.toString())));
        Optional<Tx> result = Optional.ofNullable(bytesToTx(value));
        return result;
    }

    @Override
    public void removeTx(Sha256Wrapper txHashHex) {
        _removeTx(txHashHex.toString());
        _triggerTxsRemovedEvent(Arrays.asList(txHashHex));
    }

    @Override
    public void removeTxs(List<Sha256Wrapper> txHashes) {
        txHashes.forEach( h -> _removeTx(h.toString()));
        _triggerTxsRemovedEvent(txHashes);
    }

    @Override
    public long getNumTxs() {
        AtomicLong result = new AtomicLong();
        _loopOverKeysAndRun(PREFFIX_KEY_TX, k -> result.incrementAndGet(), null);
        return result.get();
    }

    // Block-Tx Link operations:

    @Override
    public void linkTxToBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        _linkTxToBlock(txHash.toString(), blockHash.toString());
    }

    @Override
    public void unlinkTxFromBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        _unlinkTxFromBlock(txHash.toString(), blockHash.toString());
    }

    @Override
    public void unlinkTx(Sha256Wrapper txHash) {
        getBlockHashLinkedToTx(txHash).ifPresent(blockHash -> unlinkTxFromBlock(txHash, blockHash));
    }

    @Override
    public void unlinkBlock(Sha256Wrapper blockHash) {
        _unlinkBlock(blockHash.toString());
    }

    @Override
    public Optional<Sha256Wrapper> getBlockHashLinkedToTx(Sha256Wrapper txHash) {
        Optional<String> blockHash = _getBlockHashLinkedToTx(txHash.toString());
        Optional<Sha256Wrapper> result = (blockHash.isPresent())
                ? Optional.of(Sha256Wrapper.wrap(blockHash.get()))
                : Optional.empty();
        return result;
    }

    @Override
    public PaginatedResult<Sha256Wrapper> getBlockTxs(Sha256Wrapper blockHash, PaginatedRequest pagReq) {

       List<Sha256Wrapper> txsResult = new ArrayList<>();

       // Indexes in the DB we need to extract (we might not have to reach the end (endIndex), in case
       // we run out of Txs before getting there...

       long startIndex = pagReq.getNumPage() * pagReq.getPageSize();
       String startingKey = PREFFIX_KEY_BLOCKTXS + blockHash + ":";
       _loopOverKeysAndRun( startingKey,
                            startIndex,
                            Optional.of(pagReq.getPageSize()),
                            key -> txsResult.add(Sha256Wrapper.wrap(getTxHashFromKey(key))), null);
       return PaginatedResult.<Sha256Wrapper>builder().results(txsResult).build();
    }

    @Override
    public void saveBlockTxs(Sha256Wrapper blockHash, List<Tx> txs) {
        txs.forEach(tx -> {
            _saveTx(tx);
            _linkTxToBlock(tx.getHash().toString(), blockHash.toString());
        });
       _triggerTxsStoredEvent(txs);
    }

    @Override
    public void removeBlockTxs(Sha256Wrapper blockHash) {
        // The problem here is not removing the Txs belonging to this Block (which is easy), but triggering the
        // "TxsRemovedEvent" Events as we do it. We cannot trigger a single event containing the whole list of TXs,
        // since this list can be huge. So we need to PAGINATE the Event and trigger multiple events instead.
        // This is only necessary when the TX Events are active though:

        // If the TX are NOT active, we do it the "easy" way:
        if (!triggerTxEvents) {
            _loopOverKeysAndRun(PREFFIX_KEY_BLOCKTXS + blockHash, k -> _removeTx(getTxHashFromKey(k)),null);
        } else {
            // We paginate the TX of this block and we remove them page by age, while triggering an Event for each page:
            int numPage = 0;
            PaginatedRequest pagReq = PaginatedRequest.builder().numPage(0).numPage(numPage).pageSize(TX_PAG_SIZE_DEFAULT).build();
            PaginatedResult<Sha256Wrapper> blockTxs = getBlockTxs(blockHash, pagReq);
            while (blockTxs != null & blockTxs.getResults().size() > 0) {
                _triggerTxsRemovedEvent(blockTxs.getResults());
                blockTxs = getBlockTxs(blockHash, pagReq.toBuilder().numPage(++numPage).build());
            }
        }
    }

    @Override
    public BlockStoreStreamer EVENTS() {
        return this.blockStoreStreamer;
    }

    @Override
    public long getNumKeys(String keyPrefix) {
        AtomicLong result = new AtomicLong();
        _loopOverKeysAndRun(keyPrefix, k -> result.incrementAndGet(), null);
        return result.get();
    }
}
