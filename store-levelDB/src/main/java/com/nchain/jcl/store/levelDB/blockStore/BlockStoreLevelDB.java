package com.nchain.jcl.store.levelDB.blockStore;

import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockStore.events.BlockStoreStreamer;
import com.nchain.jcl.store.keyValue.blockStore.BlockStoreKeyValue;
import com.nchain.jcl.store.keyValue.common.KeyValueIterator;
import com.nchain.jcl.store.levelDB.common.LevelDBIterator;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;
import java.util.function.Function;


import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * BlockStore Implementation based on LevelDB Database.
 * It extends the BlockStoreKeyValue Interface, so it already contains most of the business logic, only
 * imnplementation-specific details are defined here.
 *
 *  - In LevelDB, each Database Entrie returned by a LevelDB iterator is a Map.Entry<byte[],byte[]>
 *  - LevelDB does NOT support Transactions, so we use "Object" as the Transaction type, and all the methods that
 *    are supposed to create/commit/rollback transactions do nothing.
 */
@Slf4j
public class BlockStoreLevelDB implements BlockStoreKeyValue<Map.Entry<byte[], byte[]>, Object> {

    // Working Folder where the LevelDB files will be stored. Its an inner folder inside the working folder defined
    // by the RuntimeConfiguration
    private static final String LEVELDB_FOLDER = "store/levelDB";

    // A separator for full keys, made from composing smaller sub-keys:
    public static final String KEY_SEPARATOR = "\\";

    // A lock (used by some methods, to ensure Thread-safety):
    @Getter private Object lock = new Object();

    // LevelDB instance:
    protected final DB levelDBStore;

    // Configuration
    @Getter private BlockStoreLevelDBConfig config;
    @Getter private boolean triggerBlockEvents;
    @Getter private boolean triggerTxEvents;

    // Events Configuration:
    @Getter protected final EventBus eventBus;
    private final ExecutorService executorService;
    private final BlockStoreStreamer blockStoreStreamer;

    @Builder
    public BlockStoreLevelDB(@NonNull BlockStoreLevelDBConfig  config,
                             boolean triggerBlockEvents,
                             boolean triggerTxEvents) throws RuntimeException {
        try {
            this.config = config;
            this.triggerBlockEvents = triggerBlockEvents;
            this.triggerTxEvents = triggerTxEvents;

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

    // Convenience method...
    private String castToString(Object obj) {
        if (obj instanceof String) return (String) obj;
        if (obj instanceof byte[]) return new String((byte[]) obj);
        throw new RuntimeException("Type not convertible to String");
    }


    @Override
    public byte[] fullKey(Object ...subKeys) {
        if (subKeys == null) return null;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < subKeys.length; i++) {
            if (subKeys[i] != null)
                result.append((i > 0) ? KEY_SEPARATOR + castToString(subKeys[i]) : castToString(subKeys[i]));
        }
        return result.toString().getBytes();
    }

    @Override public void   save(Object tr, byte[] key, byte[] value)                       { levelDBStore.put(key, value); }
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
    @Override public byte[] fullKeyForBlockTx(Object tr, String blockHash, String txHash)   { return fullKey(fullKeyForBlocks(tr), keyForBlockDir(blockHash), keyForBlockTx(txHash)); }
    @Override public byte[] fullKeyForBlockTx(Object tr, byte[] blockDirFullKey,
                                              String txHash)                                { return fullKey(blockDirFullKey, keyForBlockTx(txHash));}
    @Override public byte[] fullKeyForBlockDir(Object tr, String blockHash)                 { return fullKey(fullKeyForBlocks(tr), keyForBlockDir(blockHash)); }
    @Override public byte[] fullKeyForTxs(Object tr)                                        { return fullKey(DIR_BLOCKCHAIN, config.getNetworkId(), DIR_TXS); }
    @Override public byte[] fullKeyForTx(Object tr, String txHash)                          { return fullKey(fullKeyForTxs(tr), keyForTx(txHash)); }
    @Override public byte[] fullKeyForTxBlock(Object tr, String txHash, String blockHash)   { return fullKey(fullKeyForTxs(tr), keyForTxBlock(txHash, blockHash)); }
    @Override public byte[] keyStartingWith(byte[] preffix)                                 { return preffix; }

    @Override public byte[] fullKeyForBlocks()                                              { return fullKey(DIR_BLOCKCHAIN, config.getNetworkId(), DIR_BLOCKS);}
    @Override public byte[] fullKeyForTxs()                                                 { return fullKey(DIR_BLOCKCHAIN, config.getNetworkId(), DIR_TXS);}
    @Override public BlockStoreStreamer EVENTS()                                            { return this.blockStoreStreamer; }


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
        log.info(" - working dir: " + Paths.get(config.getWorkingFolder().toString(), LEVELDB_FOLDER).toAbsolutePath());
    }

    @Override
    public void stop() {
        synchronized (getLock()) {
            try {
                this.levelDBStore.close();
                this.executorService.shutdownNow();
            } catch (IOException ioe) {
                log.error(ioe.getMessage(), ioe);
                throw new RuntimeException(ioe);
            }
        } // synchronized
    }

    @Override
    public void printKeys() {
        DBIterator it = levelDBStore.iterator();
        it.seekToFirst();
        log.info(" > DB Content:");
        while (it.hasNext()) log.info(" > " + new String(it.next().getKey()));
    }
}
