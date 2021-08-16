/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.foundationDB.blockStore;

import com.apple.foundationdb.*;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;
import io.bitcoinsv.jcl.store.blockStore.BlockStore;
import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreStreamer;
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata;
import io.bitcoinsv.jcl.store.foundationDB.common.FDBIterator;
import io.bitcoinsv.jcl.store.foundationDB.common.FDBSafeIterator;
import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValue;
import io.bitcoinsv.jcl.store.keyValue.common.KeyValueIterator;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.bitcoin.api.base.Tx;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;



/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * BlockStore Implementation based on FoundationDB Database.
 * It extends the BlockStoreKeyValue Interface, so it already contains most of the business logic, only
 * imnplementation-specific details are defined here.
 *
 *  - In FoundationDB, each Database Entrie returned by a LevelDB iterator is KeyValue
 */
public class BlockStoreFDB implements BlockStoreKeyValue<KeyValue, Transaction>, BlockStore {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlockStoreFDB.class);
    // Configuration
    private BlockStoreFDBConfig config;
    private final boolean triggerBlockEvents;
    private final boolean triggerTxEvents;

    // A lock (used by some methods, to ensure Thread-safety):
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    // DB Connection:
    protected FDB fdb;
    protected Database db;

    // Directory Layers within the DB:
    protected DirectoryLayer dirLayer;
    protected DirectorySubspace netDir;
    protected DirectorySubspace blockchainDir;
    protected DirectorySubspace blocksDir;
    protected DirectorySubspace blocksMetadataDir;
    protected DirectorySubspace txsDir;

    // Events Streaming Configuration:
    protected final ExecutorService eventBusExecutor;
    protected final EventBus eventBus;
    private final BlockStoreStreamer blockStoreStreamer;

    // Executor to trigger Async Methods:
    private ExecutorService executor;

    // MetadataClass linked to Blocks;
    private Class<? extends Metadata> blockMetadataClass;

    public BlockStoreFDB(@Nonnull BlockStoreFDBConfig config,
                         boolean triggerBlockEvents,
                         boolean triggerTxEvents,
                         Class<? extends Metadata> blockMetadataClass) {
        this.config = config;
        this.triggerBlockEvents = triggerBlockEvents;
        this.triggerTxEvents = triggerTxEvents;
        this.blockMetadataClass = blockMetadataClass;

        // Events Configuration:
        this.eventBusExecutor = ThreadUtils.getCachedThreadExecutorService("BlockStore-FoundationDB");
        this.eventBus = EventBus.builder().executor(this.eventBusExecutor).build();
        this.blockStoreStreamer = new BlockStoreStreamer(this.eventBus);

        // Executor (to trigger async methods)
        //this.executor = Executors.newSingleThreadExecutor();
        this.executor = Executors.newFixedThreadPool(50);
    }

    // Convenience method:
    private byte[] castToBytes(Object obj) {
        if (obj instanceof DirectorySubspace)   return ((DirectorySubspace) obj).getKey();
        if (obj instanceof byte[])              return ((byte[]) obj);
        if (obj instanceof String)              return ((String) obj).getBytes();
        throw new RuntimeException("Type not convertible to byte[]");
    }

    @Override
    public ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * This method initializes the connection to the DB and it initialises the internal Directory structure of the Data.
     * If no cluster File is specified in the Configuration, then the default location is used (check documentation for
     * the default cluster file location depending on the OS):
     * <br />
     * <a href="https://apple.github.io/foundationdb/administration.html" >Foundation DB Administration</a>
     */
    @Override
    public void start() {
        // We init the DB Connection...
        getLogger().debug("FDB Connection: Setting up version...");
        fdb = FDB.selectAPIVersion(config.getApiVersion());
        getLogger().debug("FDB Connection: Connecting to the DB...");
        db = (config.getClusterFile() == null)? fdb.open() : fdb.open(config.getClusterFile());
        getLogger().debug("FDB Connection: Connecting established.");
        // We initialize the Directory Layer and the directory structure:
        initDirectoryStructure();
    }

    /* It creates the Directory Layer structure */
    protected void initDirectoryStructure() {
        dirLayer = new DirectoryLayer();
        db.run( tr -> {
            blockchainDir     = dirLayer.createOrOpen(tr, Arrays.asList(DIR_BLOCKCHAIN)).join();
            netDir            = blockchainDir.createOrOpen(tr, Arrays.asList(config.getNetworkId())).join();
            blocksDir         = netDir.createOrOpen(tr, Arrays.asList(DIR_BLOCKS)).join();
            blocksMetadataDir = blocksDir.createOrOpen(tr, Arrays.asList(DIR_METADATA)).join();
            txsDir            = netDir.createOrOpen(tr, Arrays.asList(DIR_TXS)).join();

            // We print out the DB Structure and general info about the configuration:
            log.info("JCL-Store Configuration:");
            log.info(" - FoundationDB Implementation");
            log.info(" - Network : " + config.getNetworkId());

            return null;
        });
    }

    @Override
    public void stop() {
        try {
            getLock().writeLock().lock();
            log.info("FDB-Store Stopping...");
            this.db.close();
            this.eventBusExecutor.shutdownNow();
            this.executor.shutdownNow();
            log.info("FDB-Store Stopped.");
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    public byte[] fullKey(Object ...subKeys) {
        if (subKeys == null) return null;

        byte[] result = new byte[0];
        for (Object subKey : subKeys) {
            if (subKey != null) {
                byte[] bytesFromSubList = castToBytes(subKey);
                result = Bytes.concat(result, bytesFromSubList);
            }
        }
        // Now we build the result:
        return result;
    }

    @Override public void save(Transaction tr, byte[] key, byte[] value) {
        tr.set(key, value);
    }

    @Override public void remove(Transaction tr, byte[] key) {
        tr.clear(key);
    }

    @Override public byte[] read(Transaction tr, byte[] key) {
        try {
            return tr.get(key).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<byte[]> readAsync(Transaction tr, byte[] key) {
        return tr.get(key);
    }


    @Override public Transaction createTransaction() {
        return db.createTransaction();
    }

    @Override public void   rollbackTransaction(Transaction tr) {
        // NO rollback in FoundationdB????
    }
    @Override public void   commitTransaction(Transaction tr) {
        tr.commit().join();
        //((Transaction) tr).commit();
        tr.close();
    }

    @Override public Logger getLogger()                                                         { return log;}
    @Override public byte[] keyFromItem(KeyValue item)                                          { return item.getKey();}

    @Override public byte[] fullKeyForBlocks(Transaction tr )                                    { return blocksDir.getKey();}
    @Override public byte[] fullKeyForBlock(Transaction tr, String blockHash)                    { return fullKey(blocksDir, keyForBlock(blockHash));}
    @Override public byte[] fullKeyForBlockNumTxs(Transaction tr, String blockHash)              { return fullKey(blocksDir, keyForBlockNumTxs(blockHash));}
    @Override public byte[] fullKeyForBlockTxIndex(Transaction tr, String blockHash)             { return fullKey(fullKeyForBlocks(), keyForBlockTxIndex(blockHash));}

    @Override public byte[] fullKeyForBlockTx(Transaction tr, String blockHash, String txHash, long txIndex) {
        return fullKey(fullKeyForBlockDir(tr, blockHash), keyForBlockTx(txHash, txIndex));
    }

    @Override public byte[] fullKeyForBlockTx(Transaction tr, byte[] blockDirFullKey, String txHash, long txIndex) {
        return fullKey(blockDirFullKey, keyForBlockTx(txHash, txIndex));}

    @Override public byte[] fullKeyForTxs(Transaction tr)                                        { return txsDir.getKey();}
    @Override public byte[] fullKeyForTx(Transaction tr, String txHash)                          { return fullKey(txsDir, keyForTx(txHash));}
    @Override public byte[] fullKeyForTxBlock(Transaction tr, String txHash, String blockHash)   { return fullKey(txsDir, keyForTxBlock(txHash, blockHash));}

    @Override public byte[] fullKeyForBlockDir(Transaction tr, String blockHash) {
        try {
            DirectorySubspace blockDir = blocksDir.createOrOpen(tr, Arrays.asList(blockHash)).get();
            byte[] result = blockDir.getKey();
            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public byte[] fullKeyForBlocksMetadata(Transaction tr)                            { return blocksMetadataDir.getKey(); }
    @Override public byte[] fullKeyForBlockMetadata(Transaction tr, String blockHash)           { return fullKey(fullKeyForBlocksMetadata(tr), keyForBlockMetadata(blockHash)); }
    @Override public byte[] fullKeyForBlocks()                                                  { return blocksDir.getKey();}
    @Override public byte[] fullKeyForTxs()                                                     { return txsDir.getKey();}
    @Override public BlockStoreStreamer EVENTS()                                                { return this.blockStoreStreamer; }

    @Override public Class<? extends Metadata>  getMetadataClassForBlocks()                     { return this.blockMetadataClass; }

    @Override
    public void removeBlockDir(String blockHash) {
        db.run(tr -> {
            blocksDir.remove(tr, Arrays.asList(keyForBlockDir(blockHash))).join();
            return null;
        });
    }

    @Override
    public <I> KeyValueIterator<I,Transaction> getIterator(byte[] startingWith,
                                                           byte[] endingWith,
                                                           BiPredicate<Transaction, byte[]> keyVerifier,
                                                           Function<KeyValue, I> buildItemBy) {
        return FDBSafeIterator.<I>safeBuilder()
                .database(this.db)
                .startingWithPreffix(startingWith)
                .endingWithSuffix(endingWith)
                .keyIsValidWhen(keyVerifier)
                .buildItemBy(buildItemBy)
                .build();
    }

    @Override
    public <I> KeyValueIterator<I,Transaction> getIterator( Transaction tr,
                                                            byte[] startingWith,
                                                            byte[] endingWith,
                                                            BiPredicate<Transaction, byte[]> keyVerifier,
                                                            Function<KeyValue, I> buildItemBy) {
        return FDBIterator.<I>builder()
                .currentTransaction(tr)
                .startingWithPreffix(startingWith)
                .endingWithSuffix(endingWith)
                .keyIsValidWhen(keyVerifier)
                .buildItemBy(buildItemBy)
                .build();
    }


    @Override

    public List<Tx> _saveTxsIfNotExist(Transaction tr, List<Tx> txs) {
        List<Tx> result = new ArrayList<>();
        try {
            // to check whether the Tx exists in the DB (we use async read):
            Map<String, CompletableFuture<byte[]>> readFutures = new ConcurrentHashMap<>();

            // to calculate and keep the KEY for each Tx, to do it only once:
            Map<String, byte[]> txKeys = new ConcurrentHashMap<>();

            // We check if the Txs exists in the DB (we launch the queries, will collect the results later on)
            for (Tx tx : txs) {
                String txHash = tx.getHashAsString();
                byte[] txKey = fullKeyForTx(tr, txHash);
                txKeys.put(txHash, txKey);
                readFutures.put(txHash, readAsync(tr, txKey));
            }
            // Now we loop over the futures checking the results, and if the Tx does NOT exists we insert it
            // and add it to the result:
            for (Tx tx: txs) {
                String txHash = tx.getHashAsString();
                byte[] value = readFutures.get(txHash).get();
                if (value == null) {
                    save(tr, txKeys.get(txHash), bytes(tx));
                    result.add(tx);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private void printDir(Transaction tr, DirectorySubspace parentDir, int level, boolean showDirContent) {

        String tabulation = Strings.repeat("  ", level); // deeper elements go further to the right when printed
        log.info(tabulation + "\\" + Iterables.getLast(parentDir.getPath()).toUpperCase() + " " + Arrays.toString(parentDir.getKey())) ;
        if (showDirContent) {
            // We iterate over the Keys stored within this Directory:
            Iterator<byte[]> keysIt = FDBIterator.<byte[]>builder()
                    .currentTransaction(tr)
                    .startingWithPreffix(fullKey(parentDir))
                    .buildItemBy((kv) -> kv.getKey())
                    .build();
            while (keysIt.hasNext()) {
                byte[] key = keysIt.next();
                log.info(tabulation + " - " + new String(key) + " " + Arrays.toString(key));
            }
        }
        // We do the same for its Subfolders
        level++;
        for (String dir : parentDir.list(tr).join()) {
            printDir(tr, parentDir.open(tr, Arrays.asList(dir)).join(), level, showDirContent);
        }
    }

    @Override
    public void loopOverKeysAndRun(KeyValueIterator<byte[], Transaction> iterator,
                                    Long startingKeyIndex,
                                    Optional<Long> maxKeysToProcess,
                                    BiConsumer<Transaction, byte[]> taskForKey,
                                    BiConsumer<Transaction, List<byte[]>> taskForAllKeys) {
        BlockStoreKeyValue.super.loopOverKeysAndRun(iterator, startingKeyIndex, maxKeysToProcess, taskForKey, taskForAllKeys);
        // After the iterator is executed (we assume its a "FDBSafeIterator", we need to make sure that the Transaction
        // is committed, since that might NOT be the same Transaction as the one when the lop started (the Iterator
        // might have had to reset it in order not to brake the FoundationDB limitations)
        iterator.getCurrentTransaction().commit().join();
        iterator.getCurrentTransaction().close();
    }

    @Override
    public void clear() {
        db.run(tr -> {
            try {
                // We remove The Blocks and Txs layers
                if (blockchainDir.exists(tr).get()) blockchainDir.remove(tr).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
        // And we init again the Directory Layer structure:
        initDirectoryStructure();
    }

    @Override
    public void printKeys() {
        log.debug("DB Content:");
        db.run(tr -> {
            printDir(tr, blockchainDir, 0, true);
            return null;
        });
    }

    public BlockStoreFDBConfig getConfig()      { return this.config; }
    public boolean isTriggerBlockEvents()       { return this.triggerBlockEvents; }
    public boolean isTriggerTxEvents()          { return this.triggerTxEvents; }
    public ReadWriteLock getLock()              { return this.lock; }
    public FDB getFdb()                         { return this.fdb; }
    public Database getDb()                     { return this.db; }
    public DirectorySubspace getNetDir()        { return this.netDir; }
    public DirectorySubspace getBlockchainDir() { return this.blockchainDir; }
    public DirectorySubspace getBlocksDir()     { return this.blocksDir; }
    public DirectorySubspace getTxsDir()        { return this.txsDir; }
    public EventBus getEventBus()               { return this.eventBus; }

    public static BlockStoreFDBBuilder builder() { return new BlockStoreFDBBuilder(); }

    /**
     * Builder
     */
    public static class BlockStoreFDBBuilder {
        private @Nonnull BlockStoreFDBConfig config;
        private boolean triggerBlockEvents;
        private boolean triggerTxEvents;
        private Class<? extends Metadata> blockMetadataClass;

        BlockStoreFDBBuilder() {
        }

        public BlockStoreFDB.BlockStoreFDBBuilder config(@Nonnull BlockStoreFDBConfig config) {
            this.config = config;
            return this;
        }

        public BlockStoreFDB.BlockStoreFDBBuilder triggerBlockEvents(boolean triggerBlockEvents) {
            this.triggerBlockEvents = triggerBlockEvents;
            return this;
        }

        public BlockStoreFDB.BlockStoreFDBBuilder triggerTxEvents(boolean triggerTxEvents) {
            this.triggerTxEvents = triggerTxEvents;
            return this;
        }

        public BlockStoreFDB.BlockStoreFDBBuilder blockMetadataClass(Class<? extends Metadata> blockMetadataClass) {
            this.blockMetadataClass = blockMetadataClass;
            return this;
        }

        public BlockStoreFDB build() {
            return new BlockStoreFDB(config, triggerBlockEvents, triggerTxEvents, blockMetadataClass);
        }
    }
}
