package com.nchain.jcl.store.foundationDB.blockStore;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.tuple.Tuple;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.base.tools.thread.ThreadUtils;
import com.nchain.jcl.store.blockStore.BlockStore;
import com.nchain.jcl.store.blockStore.events.BlockStoreStreamer;
import com.nchain.jcl.store.foundationDB.common.FDBIterator;
import com.nchain.jcl.store.foundationDB.common.FDBSafeIterator;
import com.nchain.jcl.store.keyValue.blockStore.BlockStoreKeyValue;
import com.nchain.jcl.store.keyValue.common.KeyValueIterator;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

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
@Slf4j
public class BlockStoreFDB implements BlockStoreKeyValue<KeyValue, Transaction>, BlockStore {

    // Configuration
    @Getter private BlockStoreFDBConfig config;
    @Getter private final boolean triggerBlockEvents;
    @Getter private final boolean triggerTxEvents;

    // A lock (used by some methods, to ensure Thread-safety):
    @Getter private ReadWriteLock lock = new ReentrantReadWriteLock();

    // DB Connection:
    @Getter protected FDB fdb;
    @Getter protected Database db;

    // Directory Layers within the DB:
    protected DirectoryLayer dirLayer;
    @Getter protected DirectorySubspace netDir;
    @Getter protected DirectorySubspace blockchainDir;
    @Getter protected DirectorySubspace blocksDir;
    @Getter protected DirectorySubspace txsDir;

    // Events Streaming Configuration:
    protected final ExecutorService executorService;
    @Getter protected final EventBus eventBus;
    private final BlockStoreStreamer blockStoreStreamer;

    @Builder
    public BlockStoreFDB(@NonNull BlockStoreFDBConfig config,
                         boolean triggerBlockEvents, boolean triggerTxEvents) {
        this.config = config;
        this.triggerBlockEvents = triggerBlockEvents;
        this.triggerTxEvents = triggerTxEvents;

        // Events Configuration:
        this.executorService = ThreadUtils.getThreadPoolExecutorService("BlockStore-FoundationDB");
        this.eventBus = EventBus.builder().executor(this.executorService).build();
        this.blockStoreStreamer = new BlockStoreStreamer(this.eventBus);


    }

    // Convenience method:
    private byte[] castToBytes(Object obj) {
        if (obj instanceof DirectorySubspace)   return ((DirectorySubspace) obj).getKey();
        if (obj instanceof byte[])              return ((byte[]) obj);
        if (obj instanceof String)              return ((String) obj).getBytes();
        throw new RuntimeException("Type not convertible to byte[]");
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
            blockchainDir    = dirLayer.createOrOpen(tr, Arrays.asList(DIR_BLOCKCHAIN)).join();
            netDir           = blockchainDir.createOrOpen(tr, Arrays.asList(config.getNetworkId())).join();
            blocksDir        = netDir.createOrOpen(tr, Arrays.asList(DIR_BLOCKS)).join();
            txsDir           = netDir.createOrOpen(tr, Arrays.asList(DIR_TXS)).join();

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
            this.executorService.shutdownNow();
            log.info("FDB-Store Stopped.");
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override public byte[] keyStartingWith(byte[] preffix) {
        byte[] resultBytes = new byte[preffix.length - 1];
        System.arraycopy(preffix, 0, resultBytes, 0, preffix.length - 1);
        return resultBytes;
    }

    public byte[] keyEndingWith(byte[] suffix) {
        byte[] resultBytes = new byte[suffix.length + 1];
        System.arraycopy(suffix, 0, resultBytes, 0, suffix.length);
        return resultBytes;
    }

    @Override
    public byte[] fullKey(Object ...subKeys) {
        if (subKeys == null) return null;
        // We only work with the imtes that are not null.
        int numSubKeysNotNull = 0;
        for (Object subKey : subKeys) if (subKey != null) numSubKeysNotNull++;

        // First, we convert all the items into a byte[].
        byte[][] bytesList = new byte[numSubKeysNotNull][];
        int i = 0;
        for (Object subKey : subKeys) {
            // We are assuming that each one of the SubLists is a PARTIAL-Key. So it Should NOT Be a TUPLE. But just
            // in case, we are checking it anyway. So if any of these subLists is a TUPLE, then we just remove the first
            // and last bytes, which are the ones that make it a TUPLE...
            if (subKey != null) {
                byte[] bytesFromSubList = castToBytes(subKey);
                if (bytesFromSubList.length > 2 && bytesFromSubList[0] == 1 && bytesFromSubList[bytesFromSubList.length -1] == 0) {
                    byte[] bytesFromSubListTrimmed = new byte[bytesFromSubList.length - 2];
                    System.arraycopy(bytesFromSubList, 1, bytesFromSubListTrimmed, 0, bytesFromSubList.length - 2);
                    bytesList[i] = bytesFromSubListTrimmed;
                } else    bytesList[i] = bytesFromSubList;
                i++;
            }
        }
        return Tuple.from(bytesList).pack();
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
    @Override public byte[] fullKeyForBlockTx(Transaction tr, String blockHash, String txHash)   { return fullKey(fullKeyForBlockDir(tr, blockHash), keyForBlockTx(txHash));}
    @Override public byte[] fullKeyForBlockTx(Transaction tr, byte[] blockDirFullKey,
                                              String txHash)                                     { return fullKey(blockDirFullKey, keyForBlockTx(txHash));}
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

    @Override public byte[] fullKeyForBlocks()          { return blocksDir.getKey();}
    @Override public byte[] fullKeyForTxs()             { return txsDir.getKey();}
    @Override public BlockStoreStreamer EVENTS()        { return this.blockStoreStreamer; }

    @Override
    public void removeBlockDir(String blockHash) {
        db.run(tr -> {
            blocksDir.remove(tr, Arrays.asList(keyForBlockDir(blockHash))).join();
            return null;
        });
    }

    @Override
    public <I> KeyValueIterator<I,Transaction> getIterator( byte[] startingWith,
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
            // We remove The Blocks and Txs layers
            dirLayer.remove(tr);
            // And we init again the Directory Layer structure:
            initDirectoryStructure();
            return null;
        });
    }

    @Override
    public void printKeys() {
        log.debug("DB Content:");
        db.run(tr -> {
            printDir(tr, blockchainDir, 0, true);
            return null;
        });
    }


}
