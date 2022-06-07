package io.bitcoinsv.jcl.store.foundationDB.blockStore;

import com.apple.foundationdb.*;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.subspace.Subspace;
import com.apple.foundationdb.tuple.Tuple;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;
import io.bitcoinsv.jcl.store.blockStore.BlockStore;
import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreStreamer;
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata;
import io.bitcoinsv.jcl.store.foundationDB.common.FDBIterator;
import io.bitcoinsv.jcl.store.foundationDB.common.FDBSafeIterator;
import io.bitcoinsv.jcl.store.foundationDB.common.LargeTransaction;
import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValue;
import io.bitcoinsv.jcl.store.keyValue.common.KeyValueIterator;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
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
public class BlockStoreFDB implements BlockStoreKeyValue<KeyValue, LargeTransaction>, BlockStore {

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
    protected DirectorySubspace txsMetadataDir;
    protected DirectorySubspace txsDir;
    protected DirectorySubspace shardsDir;
    protected DirectorySubspace incompleteTxRefDir;

    // Events Streaming Configuration:
    protected final ExecutorService eventBusExecutor;
    protected final EventBus eventBus;
    private final BlockStoreStreamer blockStoreStreamer;

    // Executor to trigger Async Methods:
    private ExecutorService executor;

    // MetadataClass linked to Blocks;
    private Class<? extends Metadata> blockMetadataClass;

    // Metadata class linked to Txs
    private Class<? extends Metadata> txMetadataClass;

    public BlockStoreFDB(@Nonnull BlockStoreFDBConfig config,
                         boolean triggerBlockEvents,
                         boolean triggerTxEvents,
                         Class<? extends Metadata> blockMetadataClass,
                         Class<? extends Metadata> txMetadataClass) {
        this.config = config;
        this.triggerBlockEvents = triggerBlockEvents;
        this.triggerTxEvents = triggerTxEvents;
        this.blockMetadataClass = blockMetadataClass;
        this.txMetadataClass = txMetadataClass;

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
        if (obj instanceof Subspace)            return ((Subspace) obj).getKey();
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
        //If the previous shutdown was unclean, it's possible that we may be left with some corrupted data.
        clearCorruptData();
    }

    /* It creates the Directory Layer structure */
    protected void initDirectoryStructure() {
        dirLayer = new DirectoryLayer();
        db.run( tr -> {
            try {
                blockchainDir = dirLayer.createOrOpen(tr, Arrays.asList(DIR_BLOCKCHAIN)).get();
                netDir = blockchainDir.createOrOpen(tr, Arrays.asList(config.getNetworkId())).get();
                blocksDir = netDir.createOrOpen(tr, Arrays.asList(DIR_BLOCKS)).get();
                blocksMetadataDir = blocksDir.createOrOpen(tr, Arrays.asList(DIR_METADATA)).get();
                txsDir = netDir.createOrOpen(tr, Arrays.asList(DIR_TXS)).get();
                txsMetadataDir = txsDir.createOrOpen(tr, Arrays.asList(DIR_METADATA)).get();
                shardsDir = netDir.createOrOpen(tr, Arrays.asList("SHARDS")).get();
                incompleteTxRefDir = netDir.createOrOpen(tr, Arrays.asList("REFERENCES")).get();
            } catch (Exception ex){
                throw new RuntimeException(ex);
            }
            // We print out the DB Structure and general info about the configuration:
            log.info("JCL-Store Configuration:");
            log.info(" - FoundationDB Implementation");
            log.info(" - Network : " + config.getNetworkId());

            return null;
        });
    }

    /**
     * Due to the LargeTransaction class not being atomic, it's possible that data that was due to be saved or removed over two transactions has only been half complete.
     * LargeTransaction therefore saves an entry in the {netDir}/references folder until we can be sure the transaction has been completely saved, in which it is then removed.
     * If an entry is to be found within this folder, we know the transaction was not completely saved, likely due to an unclean shutdown and therefore we will remove everything that
     * has been saved to restore the data to how it was.
     */
    private void clearCorruptData(){
        LargeTransaction tr = new LargeTransaction(db, incompleteTxRefDir, BlockStoreFDBConfig.TRANSACTION_MAX_VALUE_SIZE_BYTES);

        printKeys();

        tr.getRange(incompleteTxRefDir.range()).forEach(keyValue -> {
            tr.clearReferenceData(keyValue.getKey(), keyValue.getValue());

            log.debug("Corrupt entry: " + Tuple.from(keyValue.getValue()) + " removed from database");
        });

        try{
            tr.commit().get();
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
        printKeys();
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



    @Override public void save(LargeTransaction tr, byte[] key, byte[] value) {
        if(value.length > BlockStoreFDBConfig.TRANSACTION_MAX_VALUE_SIZE_BYTES){
            saveBlob(tr, key, value);
        } else {
            tr.set(key, value);
        }
    }

    private void saveBlob(LargeTransaction tr, byte[] key, byte[] value){
        int numChunks = (value.length + BlockStoreFDBConfig.TRANSACTION_VALUE_CHUNK_SIZE_BYTES) / BlockStoreFDBConfig.TRANSACTION_VALUE_CHUNK_SIZE_BYTES;
        int chunkSize = (value.length + numChunks)/numChunks;

        tr.set(key, new byte[0]);

        Subspace itemShardDir = shardsDir.subspace(Tuple.from(key));

        for(int i = 0; i * chunkSize < value.length; i++){
            int start = i * chunkSize;
            int end  = (i+1) * chunkSize <= value.length ? (i+1)*chunkSize : value.length;

            tr.set(fullKey(itemShardDir, Tuple.from(start).pack()), Arrays.copyOfRange(value, start, end));
        }
    }

    @Override public void remove(LargeTransaction tr, byte[] key) {
        //remove the main key entry
        tr.clear(key);

        //remove chunks within the keys shard dir if they have been sharded
        Subspace itemShardDir = shardsDir.subspace(Tuple.from(key));
        tr.clear(itemShardDir.range());
    }

    @Override public byte[] read(LargeTransaction tr, byte[] key) {
        //every entry has a main key/value, or just a key if it's a blob
        byte[] value;
        try {
            value = tr.get(key).get();
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }

        if(value == null){
            //no entry found
            return null;
        }

        if(value.length == 0){
            //value is a blob
           return readBlob(tr, key);
        }

        return value;
    }

    private byte[] readBlob(LargeTransaction tr, byte[] key) {
        ByteArrayWriter byteArrayWriter = new ByteArrayWriter();

        Subspace itemShardDir = shardsDir.subspace(Tuple.from(key));

        for(KeyValue kv : tr.getRange(itemShardDir.range())){
            byteArrayWriter.write(kv.getValue());
        }

        return byteArrayWriter.reader().getFullContent();
    }

    public CompletableFuture<byte[]> readAsync(LargeTransaction tr, byte[] key) {
        return CompletableFuture.supplyAsync(() -> read(tr, key));
    }


    @Override public LargeTransaction createTransaction() {
        return new LargeTransaction(db, incompleteTxRefDir, BlockStoreFDBConfig.TRANSACTION_MAX_SIZE_BYTES);
    }

    @Override public void   rollbackTransaction(LargeTransaction tr) {
        // NO rollback in FoundationdB????
    }
    @Override public void   commitTransaction(LargeTransaction tr) {
        try {
            tr.commit().get();
            tr.close();
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override public Logger getLogger()                                                          { return log;}
    @Override public byte[] keyFromItem(KeyValue item)                                           { return item.getKey();}

    @Override public byte[] fullKeyForBlocks(LargeTransaction tr )                                    { return fullKey(blocksDir);}
    @Override public byte[] fullKeyForBlock(LargeTransaction tr, String blockHash)                    { return fullKey(blocksDir, keyForBlock(blockHash));}
    @Override public byte[] fullKeyForBlockNumTxs(LargeTransaction tr, String blockHash)              { return fullKey(fullKeyForBlock(tr, blockHash), keyForBlockNumTxs(blockHash));}
    @Override public byte[] fullKeyForBlockTxIndex(LargeTransaction tr, String blockHash)             { return fullKey(fullKeyForBlock(tr, blockHash), keyForBlockTxIndex(blockHash));}

    @Override public byte[] fullKeyForBlockTx(LargeTransaction tr, String blockHash, String txHash, long txIndex) {
        return fullKey(fullKeyForBlock(tr, blockHash), keyForBlockTx(txHash, txIndex));
    }

    @Override public byte[] fullKeyForBlockTx(LargeTransaction tr, byte[] blockDirFullKey, String txHash, long txIndex) {
        return fullKey(blockDirFullKey, keyForBlockTx(txHash, txIndex));}

    @Override public byte[] fullKeyForTxs(LargeTransaction tr)                                        { return fullKey(txsDir);}
    @Override public byte[] fullKeyForTx(LargeTransaction tr, String txHash)                          { return fullKey(txsDir, keyForTx(txHash)); }
    @Override public byte[] fullKeyForTxBlock(LargeTransaction tr, String txHash, String blockHash)   { return fullKey(txsDir, keyForTxBlock(txHash, blockHash));}

    @Override public byte[] fullKeyForBlockDir(LargeTransaction tr, String blockHash) {
        return fullKey(blocksDir, blockHash);
    }

    @Override public byte[] fullKeyForTxsMetadata(LargeTransaction tr)                                 { return fullKey(txsMetadataDir); }
    @Override public byte[] fullKeyForTxMetadata(LargeTransaction tr, String txHash)                  { return fullKey(fullKeyForTxsMetadata(tr), keyForTxMetadata(txHash));}

    @Override public byte[] fullKeyForBlocksMetadata(LargeTransaction tr)                            { return fullKey(blocksMetadataDir); }
    @Override public byte[] fullKeyForBlockMetadata(LargeTransaction tr, String blockHash)           { return fullKey(fullKeyForBlocksMetadata(tr), keyForBlockMetadata(blockHash)); }
    @Override public byte[] fullKeyForBlocks()                                                  { return fullKey(blocksDir);}
    @Override public byte[] fullKeyForTxs()                                                     { return fullKey(txsDir);}
    @Override public byte[] fullKeyForOrphanBlockHash(LargeTransaction tr, String blockHash)         { return fullKey(fullKeyForBlocks(), keyForOrphanBlockHash(blockHash));}
    @Override public BlockStoreStreamer EVENTS()                                                { return this.blockStoreStreamer; }

    @Override public Class<? extends Metadata>  getMetadataClassForBlocks()                     { return this.blockMetadataClass; }
    @Override public Class<? extends Metadata>  getMetadataClassForTxs()                     { return this.txMetadataClass; }

    @Override
    public void removeBlockDir(String blockHash) {}

    @Override
    public <I> KeyValueIterator<I,LargeTransaction> getIterator( byte[] startingWith,
                                                byte[] endingWith,
                                                BiPredicate<LargeTransaction, byte[]> keyVerifier,
                                                Function<KeyValue, I> buildItemBy) {
        return FDBSafeIterator.<I>safeBuilder()
                .database(this.db)
                .incompleteTxsDir(incompleteTxRefDir)
                .startingWithPreffix(startingWith)
                .endingWithSuffix(endingWith)
                .keyIsValidWhen(keyVerifier)
                .buildItemBy(buildItemBy)
                .build();
    }

    @Override
    public <I> KeyValueIterator<I,LargeTransaction> getIterator( LargeTransaction tr,
                                                            byte[] startingWith,
                                                            byte[] endingWith,
                                                            BiPredicate<LargeTransaction, byte[]> keyVerifier,
                                                            Function<KeyValue, I> buildItemBy) {
        return FDBIterator.<I>builder()
                .currentTransaction(tr)
                .startingWithPreffix(startingWith)
                .endingWithSuffix(endingWith)
                .keyIsValidWhen(keyVerifier)
                .buildItemBy(buildItemBy)
                .build();
    }


    public List<Tx> _saveTxsIfNotExist(LargeTransaction tr, List<Tx> txs) {
        try {
            List<Tx> result = new ArrayList<>();
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
            for (Tx tx : txs) {
                String txHash = tx.getHashAsString();
                byte[] value = readFutures.get(txHash).get();
                if (value == null) {
                    save(tr, txKeys.get(txHash), bytes(tx));
                    result.add(tx);
                }
            }

            return result;

        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private void printDir(LargeTransaction tr, DirectorySubspace parentDir, int level, boolean showDirContent) {
        String tabulation = Strings.repeat("  ", level); // deeper elements go further to the right when printed
        log.info(tabulation + "\\" + Iterables.getLast(parentDir.getPath()).toUpperCase() + " " + Tuple.from(parentDir.getKey()));
        if (showDirContent) {
            // We iterate over the Keys stored within this Directory:
            Iterator<byte[]> keysIt = FDBIterator.<byte[]>builder()
                    .currentTransaction(tr)
                    .startingWithPreffix(fullKey(parentDir))
                    .buildItemBy((kv) -> kv.getKey())
                    .build();
            while (keysIt.hasNext()) {
                byte[] key = keysIt.next();
                log.info(tabulation + " - " + Tuple.from(key) + " " + Arrays.toString(key));
            }
        }

//        // We do the same for its Subfolders
        level++;
        try {
            for (String dir : parentDir.list(tr.getCurrentTransaction()).get()) {
                printDir(tr, parentDir.open(tr.getCurrentTransaction(), Arrays.asList(dir)).get(), level, showDirContent);
            }
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }

    }

    @Override
    public void loopOverKeysAndRun(KeyValueIterator<byte[], LargeTransaction> iterator,
                                    Long startingKeyIndex,
                                    Optional<Long> maxKeysToProcess,
                                    BiConsumer<LargeTransaction, byte[]> taskForKey,
                                    BiConsumer<LargeTransaction, List<byte[]>> taskForAllKeys) {
        BlockStoreKeyValue.super.loopOverKeysAndRun(iterator, startingKeyIndex, maxKeysToProcess, taskForKey, taskForAllKeys);
        // After the iterator is executed (we assume its a "FDBSafeIterator", we need to make sure that the Transaction
        // is committed, since that might NOT be the same Transaction as the one when the lop started (the Iterator
        // might have had to reset it in order not to brake the FoundationDB limitations)
        try {
            iterator.getCurrentTransaction().commit().get();
            iterator.getCurrentTransaction().close();
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void clear() {
        db.run(tr -> {
            try {
                // We remove The Blocks and Txs layers
                if (blockchainDir.exists(tr).get()) blockchainDir.remove(tr).get();
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
            LargeTransaction largeTransaction = new LargeTransaction(db, incompleteTxRefDir, tr,  BlockStoreFDBConfig.TRANSACTION_MAX_SIZE_BYTES);
            printDir(largeTransaction, blockchainDir, 0, true);
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
        private Class<? extends Metadata> txMetadataClass;

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

        public BlockStoreFDB.BlockStoreFDBBuilder txMetadataClass(Class<? extends Metadata> txMetadataClass) {
            this.txMetadataClass = txMetadataClass;
            return this;
        }

        public BlockStoreFDB build() {
            return new BlockStoreFDB(config, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass);
        }
    }
}
