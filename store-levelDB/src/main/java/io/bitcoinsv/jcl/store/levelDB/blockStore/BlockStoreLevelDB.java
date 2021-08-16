package io.bitcoinsv.jcl.store.levelDB.blockStore;


import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreStreamer;
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata;
import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValue;
import io.bitcoinsv.jcl.store.keyValue.common.KeyValueIterator;
import io.bitcoinsv.jcl.store.levelDB.common.LevelDBIterator;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.bitcoin.api.base.Tx;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * BlockStore Implementation based on LevelDB Database.
 * It extends the BlockStoreKeyValue Interface, so it already contains most of the business logic, only
 * imnplementation-specific details are defined here.
 * <p>
 * - In LevelDB, each Database Entrie returned by a LevelDB iterator is a Map.Entry<byte[],byte[]>
 * - LevelDB does NOT support Transactions, so we use "Object" as the Transaction type, and all the methods that
 * are supposed to create/commit/rollback transactions do nothing.
 */
public class BlockStoreLevelDB implements BlockStoreKeyValue<Map.Entry<byte[], byte[]>, Object> {


    // A separator for full keys, made from composing smaller sub-keys:
    public static final String KEY_SEPARATOR = "\\";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlockStoreLevelDB.class);

    // Events Configuration:
    protected final EventBus eventBus;
    private final ExecutorService executorService;
    private final BlockStoreStreamer blockStoreStreamer;

    // LevelDB instance:
    protected DB levelDBStore;

    // A lock (used by some methods, to ensure Thread-safety):
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    // Configuration
    private BlockStoreLevelDBConfig config;
    private boolean triggerBlockEvents;
    private boolean triggerTxEvents;

    // Executor to trigger Async Methods:
    private ExecutorService executor;

    // MetadataClass linked to Blocks;
    private Class<? extends Metadata> blockMetadataClass;

    public BlockStoreLevelDB(@Nonnull BlockStoreLevelDBConfig config,
                             boolean triggerBlockEvents,
                             boolean triggerTxEvents,
                             Class<? extends Metadata> blockMetadataClass) throws RuntimeException {
        try {
            this.config = config;
            this.triggerBlockEvents = triggerBlockEvents;
            this.triggerTxEvents = triggerTxEvents;
            this.blockMetadataClass = blockMetadataClass;

            // LevelDB engine configuration. We define the Path where the LevelDB Db will be stored:
            Options options = new Options();

            Path levelDBPath = config.getWorkingFolder();
            levelDBStore = factory.open(levelDBPath.toFile(), options);

            // Events Configuration:
            this.executorService = ThreadUtils.getCachedThreadExecutorService("BlockStore-LevelDB");
            this.eventBus = EventBus.builder().executor(this.executorService).build();
            this.blockStoreStreamer = new BlockStoreStreamer(this.eventBus);

            // Executor (to trigger async methods)
            this.executor = Executors.newSingleThreadExecutor();
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    public static BlockStoreLevelDBBuilder builder() {
        return new BlockStoreLevelDBBuilder();
    }

    // Convenience method...
    private String castToString(Object obj) {
        if (obj instanceof String) return (String) obj;
        if (obj instanceof byte[]) return new String((byte[]) obj);
        throw new RuntimeException("Type not convertible to String");
    }

    @Override
    public ExecutorService getExecutor() {
        return this.executor;
    }

    @Override
    public byte[] fullKey(Object... subKeys) {
        if (subKeys == null) return null;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < subKeys.length; i++) {
            if (subKeys[i] != null)
                result.append((i > 0) ? KEY_SEPARATOR + castToString(subKeys[i]) : castToString(subKeys[i]));
        }
        return result.toString().getBytes();
    }

    @Override public void   save(Object tr, byte[] key, byte[] value){levelDBStore.put(key, value);}
    @Override public void   remove(Object tr, byte[] key)                                   { levelDBStore.delete(key); }
    @Override public byte[] read(Object tr, byte[] key)                                     { return levelDBStore.get(key); }
    @Override public Object createTransaction()                                             { return null;}
    @Override public void   commitTransaction(Object tr)                                    {}
    @Override public void   rollbackTransaction(Object tr)                                  {}

    @Override public Logger getLogger()                                                     { return log;}
    @Override public byte[] keyFromItem(Map.Entry<byte[], byte[]> item)                     { return item.getKey(); }
    @Override public byte[] fullKeyForBlocks(Object tr)                                     { return fullKey(DIR_BLOCKCHAIN, config.getNetworkId(), DIR_BLOCKS);}
    @Override public byte[] fullKeyForBlock(Object tr, String blockHash)                    { return fullKey(fullKeyForBlocks(tr), keyForBlock(blockHash)); }
    @Override public byte[] fullKeyForBlockNumTxs(Object tr, String blockHash)              { return fullKey(fullKeyForBlocks(tr), keyForBlockNumTxs(blockHash)); }
    @Override public byte[] fullKeyForBlockTxIndex(Object tr, String blockHash)             { return fullKey(fullKeyForBlocks(), keyForBlockTxIndex(blockHash));}

    @Override public byte[] fullKeyForBlockTx(Object tr, String blockHash, String txHash, long txIndex) {
        return fullKey(fullKeyForBlocks(tr), keyForBlockDir(blockHash), keyForBlockTx(txHash, txIndex));
    }

    @Override public byte[] fullKeyForBlockTx(Object tr, byte[] blockDirFullKey, String txHash, long txIndex) {
        return fullKey(blockDirFullKey, keyForBlockTx(txHash, txIndex));}

    @Override public byte[] fullKeyForBlockDir(Object tr, String blockHash)                 { return fullKey(fullKeyForBlocks(tr), keyForBlockDir(blockHash)); }
    @Override public byte[] fullKeyForBlocksMetadata(Object tr)                             { return fullKey(fullKeyForBlocks(tr), DIR_METADATA);}
    @Override public byte[] fullKeyForBlockMetadata(Object tr, String blockHash)            { return fullKey(fullKeyForBlocksMetadata(tr), keyForBlockMetadata(blockHash));}

    @Override public byte[] fullKeyForTxs(Object tr)                                        { return fullKey(DIR_BLOCKCHAIN, config.getNetworkId(), DIR_TXS); }
    @Override public byte[] fullKeyForTx(Object tr, String txHash)                          { return fullKey(fullKeyForTxs(tr), keyForTx(txHash)); }
    @Override public byte[] fullKeyForTxBlock(Object tr, String txHash, String blockHash)   { return fullKey(fullKeyForTxs(tr), keyForTxBlock(txHash, blockHash)); }

    @Override public byte[] fullKeyForBlocks()                                              { return fullKey(DIR_BLOCKCHAIN, config.getNetworkId(), DIR_BLOCKS);}
    @Override public byte[] fullKeyForTxs()                                                 { return fullKey(DIR_BLOCKCHAIN, config.getNetworkId(), DIR_TXS);}
    @Override public BlockStoreStreamer EVENTS()                                            { return this.blockStoreStreamer; }

    @Override public Class<? extends Metadata>  getMetadataClassForBlocks()                 { return this.blockMetadataClass; }

    @Override
    public <T> KeyValueIterator<T, Object> getIterator(byte[] startingWith,
                                                       byte[] endingWith,
                                                       BiPredicate<Object, byte[]> keyVerifier,
                                                       Function<Map.Entry<byte[], byte[]>, T> buildItemBy) {
        return LevelDBIterator.<T>builder()
                .database(this.levelDBStore)
                .startingWithPreffix(startingWith)
                .endingWithSuffix(endingWith)
                .keyIsValidWhen((keyVerifier != null) ? k -> keyVerifier.test(null, k) : null)
                .buildItemBy(buildItemBy)
                .build();
    }

    @Override
    public <T> KeyValueIterator<T, Object> getIterator(Object tr,
                                                       byte[] startingWith,
                                                       byte[] endingWith,
                                                       BiPredicate<Object, byte[]> keyVerifier,
                                                       Function<Map.Entry<byte[], byte[]>, T> buildItemBy) {
        return getIterator(startingWith, endingWith, keyVerifier, buildItemBy);
    }

    @Override
    public List<Tx> _saveTxsIfNotExist(Object tr, List<Tx> txs) {
        List<Tx> result = new ArrayList<>();
        // We just iterate over the TXs and insert those that does not exist
        for (Tx tx : txs) {
            byte[] txBytes = _getTxBytes(tr, tx.getHash().toString());
            if (txBytes == null || txBytes.length == 0) {
                _saveTx(tr, tx);
                result.add(tx);
            }
        }
        return result;
    }

    @Override
    public void removeBlockDir(String blockHash) {
        byte[] keyPreffix = fullKeyForBlockDir(blockHash);
        Iterator<byte[]> it = getIterator(keyPreffix, null, null, e -> keyFromItem(e));
        while (it.hasNext()) {
            byte[] key = it.next();
            remove(null, key);
        }
    }

    @Override
    public void start() {
        log.info("JCL-Store Configuration:");
        log.info(" - LevelDB Implementation");
        log.info(" - working dir: " + config.getWorkingFolder().toAbsolutePath());
    }

    @Override
    public void stop() {
        try {
            getLock().writeLock().lock();
            log.info("LevelDB-Store Stopping...");
            this.executorService.shutdownNow();
            this.executor.shutdownNow();
            this.levelDBStore.close();
            log.info("LevelDB-Store Stopped.");
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            throw new RuntimeException(ioe);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        // LevelDb stores all the info inside a Folder in the File System, so the fastest way is to just remove the
        // folder content, and re-initiate the DB...
        try {
            getLock().writeLock().lock();
            levelDBStore.close();
            Path levelDBPath = config.getWorkingFolder();
            Files.walk(levelDBPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.createDirectory(levelDBPath);
            Options options = new Options();
            levelDBStore = factory.open(levelDBPath.toFile(), options);
        } catch (IOException ioe) {
            getLogger().error("ERROR Clearing the DB", ioe);
        } finally {
            getLock().writeLock().unlock();
        }

    }

    @Override
    public void printKeys() {
        DBIterator it = levelDBStore.iterator();
        it.seekToFirst();
        log.info(" > DB Content:");
        while (it.hasNext()) log.info(" > " + new String(it.next().getKey()));
    }

    public ReadWriteLock getLock()              { return this.lock; }
    public BlockStoreLevelDBConfig getConfig()  { return this.config; }
    public boolean isTriggerBlockEvents()       { return this.triggerBlockEvents; }
    public boolean isTriggerTxEvents()          { return this.triggerTxEvents; }
    public EventBus getEventBus()               { return this.eventBus; }

    /**
     * Builder
     */
    public static class BlockStoreLevelDBBuilder {
        private @Nonnull BlockStoreLevelDBConfig config;
        private boolean triggerBlockEvents;
        private boolean triggerTxEvents;
        private Class<? extends Metadata> blockMetadataClass;

        BlockStoreLevelDBBuilder() {
        }

        public BlockStoreLevelDB.BlockStoreLevelDBBuilder config(@Nonnull BlockStoreLevelDBConfig config) {
            this.config = config;
            return this;
        }

        public BlockStoreLevelDB.BlockStoreLevelDBBuilder triggerBlockEvents(boolean triggerBlockEvents) {
            this.triggerBlockEvents = triggerBlockEvents;
            return this;
        }

        public BlockStoreLevelDB.BlockStoreLevelDBBuilder triggerTxEvents(boolean triggerTxEvents) {
            this.triggerTxEvents = triggerTxEvents;
            return this;
        }

        public BlockStoreLevelDB.BlockStoreLevelDBBuilder blockMetadataClass(Class<? extends Metadata> blockMetadataClass) {
            this.blockMetadataClass = blockMetadataClass;
            return this;
        }

        public BlockStoreLevelDB build() throws RuntimeException {
            return new BlockStoreLevelDB(config, triggerBlockEvents, triggerTxEvents, blockMetadataClass);
        }
    }
}
