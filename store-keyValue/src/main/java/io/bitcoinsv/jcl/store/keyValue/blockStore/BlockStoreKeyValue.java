package io.bitcoinsv.jcl.store.keyValue.blockStore;


import com.google.common.collect.Lists;

import io.bitcoinsv.jcl.store.blockStore.BlockStore;
import io.bitcoinsv.jcl.store.blockStore.BlocksCompareResult;
import io.bitcoinsv.jcl.store.blockStore.events.*;
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata;
import io.bitcoinsv.jcl.store.keyValue.common.HashesList;
import io.bitcoinsv.jcl.store.keyValue.common.HashesListSerializer;
import io.bitcoinsv.jcl.store.keyValue.common.KeyValueIterator;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.serialization.BitcoinSerializerUtils;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.jcl.store.blockStore.events.BlocksRemovedEvent;
import io.bitcoinsv.jcl.store.blockStore.events.BlocksSavedEvent;
import io.bitcoinsv.jcl.store.blockStore.events.TxsRemovedEvent;
import io.bitcoinsv.jcl.store.blockStore.events.TxsSavedEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This interface provides a Base for any future DB-specific implementtion based on a Key-Store Value.
 *
 * Although different implementations might have different implementation detals, all Key-Value storages must have some
 * characteristics in common:
 *
 * - The structure of the Data: The data must follow hierarchical structure:
 *   -  BLOCKCHAIN
 *      - [NET Id]
 *          - BLOCKS
 *          - TXS
 * - The Keys used to store individual Items/properties must follow the same syntax:
 *
 *   Blocks are stored under the "BLOCKS" directory:
 *      "b:[blockHash]: Stores a Whole Block Header
 *      "b_p:[block_hash]:[property]: Stores a single Block property.
 *
 *   Transactions are stored under the "TXS" directory:
 *      "tx:[tx_hash]": Stores a whole Transaction
 *      "tx_p:[txHash]:[property]": Stores a single Transaction property
 *
 *  The relationship between Blocks and Txs is stored by saving specific Keys in specific places. For each Tx belonging
 *  to a Block, 2 Keys are stored:
 *
 *  - The first one is stored under a Directory created for the Block. In that directory, a key "tx_l:[txHash]:" is
 *  stored. Its value is not important.
 *  For example, the Block "000000000000000004700fc34237707d8b92d398bb6f8bc76e421e7a42dec4b3" containing
 *   2 Tx:
 *      - "3f62b8f3447cc716900ea6fab805fa86226af7246ff0f984a8ab7e7f1d6b48ae" and
 *      - "0a3e25c984b47b3d6dd0f11e2d14159b79ff7744e9f9e6be986f812cb6ef2fef"
 *   can be  represented like this:
 *      - BLOCKS
 *          - 000000000000000004700fc34237707d8b92d398bb6f8bc76e421e7a42dec4b3
 *              "tx_l:3f62b8f3447cc716900ea6fab805fa86226af7246ff0f984a8ab7e7f1d6b48ae" (value not important)
 *              "tx_l:0a3e25c984b47b3d6dd0f11e2d14159b79ff7744e9f9e6be986f812cb6ef2fef" (value not important)
 *
 *  - The second one is stored in the "/Txs" directory, using a "tx_block:[txHash]:[blockHash:" Key. In the previous
 *  example, we suld have these 2 Keys:
 *      - TXS
 *          "tx_block:3f62b8f3447cc716900ea6fab805fa86226af7246ff0f984a8ab7e7f1d6b48ae:000000000000000004700fc34237707d8b92d398bb6f8bc76e421e7a42dec4b3"
 *          "tx_block:3f62b8f3447cc716900ea6fab805fa86226af7246ff0f984a8ab7e7f1d6b48ae:000000000000000004700fc34237707d8b92d398bb6f8bc76e421e7a42dec4b3"
 *
 *
 * @param <E>   Type of each ENTRY in the DB. Each Key-Value DB implementation usually provides Iterators that returns
 *              Entries from the DB (KeyValue in FoundationDB, a Map.Entry in LevelDb, etc).
 * @param <T>   Type of the TRANSACTION used by the DB-specific implementation. If Transactions are NOT suported, the
 *              "Object" Type can be used.
 */
public interface BlockStoreKeyValue<E,T> extends BlockStore {

    /** Configuration and reference to the logger*/
    BlockStoreKeyValueConfig getConfig();
    Logger getLogger();

    /** Events Support Operations */
    int MAX_EVENT_ITEMS = 1000;                         // max of items published on each Event
    boolean isTriggerBlockEvents();
    boolean isTriggerTxEvents();
    EventBus getEventBus();

    /** An executor to trigger Async methods: */
    ExecutorService getExecutor();

    /** Definition of the Directory structure: */
    String DIR_BLOCKCHAIN            = "blockchain";
    String DIR_BLOCKS                = "blocks";
    String DIR_TXS                   = "txs";
    String DIR_METADATA              = "metadata";

    /** preffixes/suffixes used in Keys: */
    String KEY_SEPARATOR             = ":";
    String KEY_PREFFIX_BLOCK         = "block" + KEY_SEPARATOR;       // A Whole Block
    String KEY_PREFFIX_BLOCK_PROP    = "block_p" + KEY_SEPARATOR;     // Property suffix
    String KEY_PREFFIX_TX            = "tx" + KEY_SEPARATOR;          // A whole Tx
    String KEY_PREFFIX_TX_PROP       = "tx_p" + KEY_SEPARATOR;        // Property suffix
    String KEY_PREFFIX_TX_LINK       = "tx_link" + KEY_SEPARATOR;     // A Key that represents a reference to a Tx
    String KEY_SUFFIX_BLOCK_NUMTXS   = KEY_SEPARATOR + "numTxs" + KEY_SEPARATOR;      // Property suffix: the number of Txs in a Block
    String KEY_SUFFIX_BLOCK_TXINDEX  = KEY_SEPARATOR + "txIndex" + KEY_SEPARATOR;     // Property suffix: Last txIndex used for this Block (to preserve Tx ordering)
    String KEY_PREFFIX_TX_BLOCK      = "tx_block_link" + KEY_SEPARATOR;               // Property suffix: The list of blocks this Tx is linked to
    String KEY_PREFFIX_ORPHAN_HASH = "orphan_h" + KEY_SEPARATOR;
    String KEY_PREFFIX_BLOCK_META    = "block_m" + KEY_SEPARATOR;    // Metadata linked to a Block
    String KEY_PREFFIX_TX_META       = "tx_m" + KEY_SEPARATOR; //Metadata linked to a tx

    /** This method returns a Lock that can be used to make sure Thread-safety is in place */
    ReadWriteLock getLock();

    /** Function that takes an Item from the DB and return the Key */
    byte[] keyFromItem(E item);

    /*
     * FUNCTIONS TO BE IMPLEMENTED BY DB-SPECIFIC IMPLEMENTATIONS:
     */

    /*
        Transaction Management: Extending Class will need to implement how Transactions are crated and managed. The
        Transaction TYPE ill also have to e CAST to specific Types. If no Transactions are supported by the specific
        Db, then "Object can be used and "dummy" implementations can be provided.
     */

    T  createTransaction();
    void    commitTransaction(T tr);
    void    rollbackTransaction(T tr);

    /*
        Low-Level DB Operations: Basic CRUD operations, plus a specific method to remove a whole Block Directory
     */

    void    save(T tr, byte[] key, byte[] value);
    void    remove(T tr, byte[] key);
    byte[]  read(T tr, byte[] key);
    void    removeBlockDir(String blockHash);

    List<Tx> _saveTxsIfNotExist(T tr, List<Tx> txs);

    /*
     * Functions to generate FULL Keys. A FULL fullKey can be used to Insert/Read/Remove an item from the DB, and it
     * represents a Whole Path, using the hierarchy described (see class javadoc). They take a "tr" (Transaction)
     * because some specific DB might need to perform DB operations in order to create or access specific parts of
     * this Path.
     */

    byte[] fullKeyForBlocks(T tr);
    byte[] fullKeyForBlock(T tr, String blockHash);
    byte[] fullKeyForBlockNumTxs(T tr, String blockHash);
    byte[] fullKeyForBlockTxIndex(T tr, String blockHash);
    byte[] fullKeyForBlockTx(T tr, String blockHash, String txHash, long txIndex);
    byte[] fullKeyForBlockTx(T tr, byte[] blockDirFullKey, String txHash, long txIndex);
    byte[] fullKeyForBlockDir(T tr, String blockHash);
    byte[] fullKeyForBlocksMetadata(T tr);
    byte[] fullKeyForTxsMetadata(T tr);

    Class<? extends Metadata> getMetadataClassForBlocks();
    Class<? extends Metadata> getMetadataClassForTxs();

    byte[] fullKeyForBlockMetadata(T tr, String blockHash);
    byte[] fullKeyForTxMetadata(T tr, String txHash);

    byte[] fullKeyForTxs(T tr);
    byte[] fullKeyForTx(T tr, String txHash);
    byte[] fullKeyForTxBlock(T tr, String txHash, String blockHash);
    byte[] fullKeyForBlocks();
    byte[] fullKeyForTxs();
    byte[] fullKey(Object ...subKeys);      // Returns a FULL Key that is a concatenation of the subKeys provided
    byte[] fullKeyForOrphanBlockHash(T tr, String blockHash);
    void printKeys();                       // For logging:



    /**
     * Returns an iterator that returns Items from the BD. This iterator is meant to be used for large amounts of
     * Data, that's why the Transaction is NOT one of its parameters (the iterator itself will take care of
     * creating and resetting transactions when needed)
     *
     * @param startingWith  if specified, only keys starting with this value are processed
     * @param endingWith    if specified, only keys ending with this value are processed
     * @param keyVerifier   if specified, only those keys from which this Function returns TRUE are returned.
     * @param buildItemBy   This is the function that is executed in order to return the items.
     */
    <I> KeyValueIterator<I,T> getIterator(byte[] startingWith, byte[] endingWith,
                                          BiPredicate<T, byte[]> keyVerifier,
                                          Function<E, I>  buildItemBy);

    /** Returns an iterator that uses the current transaction and is meant to be used with a small set of data */
    <I> KeyValueIterator<I,T> getIterator(T transaction, byte[] startingWith, byte[] endingWith,
                                          BiPredicate<T, byte[]> keyVerifier,
                                          Function<E, I>  buildItemBy);
    /*
     * DEFAULT IMPLEMENTATIONS:
     * ========================
     * All the functions below represent common behaviour, so the business logic is implemented here at high-level.
     */

    /*
     * Functions to generate "partial" Keys in String format
     * A partial Key can NOT be used to make an Insert in the Db, since these Keys are relative, they need to be
     * combined with other in order to create a FULL Key, which can be used to insert/read/remove items from the DB.
     */

    default String keyForBlock(String blockHash)                    { return KEY_PREFFIX_BLOCK + blockHash + KEY_SEPARATOR; }
    default String keyForBlockNumTxs(String blockHash)              { return KEY_PREFFIX_BLOCK_PROP + blockHash + KEY_SUFFIX_BLOCK_NUMTXS; }
    default String keyForBlockTxIndex(String blockHash)             { return KEY_PREFFIX_BLOCK_PROP + blockHash + KEY_SUFFIX_BLOCK_TXINDEX; }
    default String keyForTx(String txHash)                          { return KEY_PREFFIX_TX + txHash + KEY_SEPARATOR;}
    default String keyForTxBlock(String txHash, String blockHash)   { return KEY_PREFFIX_TX_BLOCK + txHash + KEY_SEPARATOR + blockHash + KEY_SEPARATOR; }
    default String keyForBlockTx(String txHash, long txIndex)       {
        /** We get the log of the number and add 1 to get the amount of digits in the number. We then convert that number to a char to use as a prefix to guarantee
         *  Lexicographical ordering. So for example, the series: 0, 1, 42, 1623463626463, which would normally appear as: 0, 1, 1623463626463, 42 (which is lexi, not numerical)
         *  would now appear as: "0, "1, #42, .1623463626463. We also add 1 to the index as 0 blows up this formula.
         *
         *  NOTES: If the db implementation does not order lexicographically, then we need to override this function
         */
        return KEY_PREFFIX_TX_LINK + Character.toString((int)Math.log10(txIndex + 1) + 1) + "" + ( txIndex + 1) + KEY_SEPARATOR + txHash + KEY_SEPARATOR;
    }
    default String keyForBlockDir(String blockHash)                 { return blockHash;}
    default String keyForOrphanBlockHash(String blockHash)          { return KEY_PREFFIX_ORPHAN_HASH + blockHash + KEY_SEPARATOR;}
    default String keyForBlockMetadata(String blockHash)            { return KEY_PREFFIX_BLOCK_META + blockHash + KEY_SEPARATOR + getMetadataClassForBlocks().getSimpleName();}
    default String keyForTxMetadata(String txHash)                  { return KEY_PREFFIX_TX_META + txHash + KEY_SEPARATOR + getMetadataClassForTxs();}

    @Override
    default long getNumKeys(String preffix)                         { return numKeys(preffix.getBytes()); }


    // A convenience method for getting the Full fullKey for a Block Directory, with specifying a DB-Transaction.
    default byte[] fullKeyForBlockDir(String blockHash) {
        AtomicReference<byte[]> result = new AtomicReference<>();
        T tr = createTransaction();
        executeInTransaction(tr, ()-> result.set(fullKeyForBlockDir(tr, blockHash)));
        return result.get();
    }

    // Returns the number of Keys starting with the preffix given
    default long numKeys(byte[] startingWith) {
        AtomicLong result = new AtomicLong();
        KeyValueIterator<byte[], T> iterator = getIterator(startingWith, null, null, this::keyFromItem);
        loopOverKeysAndRun(iterator, (tr, k) -> result.incrementAndGet(), null);
        return result.get();
    }

    /* Functions to serialize Objects: */

    default byte[] uint64ToByteArrayLE(Long value) {
        byte[] result = new byte[8];
        Utils.uint64ToByteArrayLE(value, result, 0);
        return result;
    }

    default byte[] uint32ToByteArrayLE(Integer value) {
        byte[] result = new byte[4];
        Utils.uint32ToByteArrayLE(value, result, 0);
        return result;
    }

    default byte[] bytes(HashesList chainTips)        { return HashesListSerializer.getInstance().serialize(chainTips); }
    default byte[] bytes(HeaderReadOnly header)       { return header.serialize();}
    default byte[] bytes(Tx tx)                       { return tx.serialize(); }
    default byte[] bytes(Long value)                  { return uint64ToByteArrayLE(value); }
    default byte[] bytes(Integer value)               { return uint32ToByteArrayLE(value); }
    default byte[] bytes(String value)                {
        ByteArrayWriter writer = new ByteArrayWriter();
        BitcoinSerializerUtils.serializeVarStr(value, writer);
        return writer.reader().getFullContentAndClose();
    }


    /* Functions to deserialize Objects: */

    default boolean         isBytesOk(byte[] bytes)       { return (bytes != null && bytes.length > 0);}
    default HeaderReadOnly  toBlockHeader(byte[] bytes)   { return (isBytesOk(bytes)) ? new HeaderBean(bytes) : null;}
    default Tx              toTx(byte[] bytes)            { return (isBytesOk(bytes)) ? new TxBean(bytes) : null;}
    default HashesList      toHashes(byte[] bytes)        { return (isBytesOk(bytes)) ? HashesListSerializer.getInstance().deserialize(bytes) : null;}
    default Long            toLong(byte[] bytes)          { return (isBytesOk(bytes)) ? Utils.readInt64(bytes, 0) : null;}
    default Integer         toInt(byte[] bytes)           { return (isBytesOk(bytes)) ? (int) Utils.readUint32(bytes, 0) : null;}

    default String          toString(byte[] bytes) {
        if (!isBytesOk(bytes)) return null;
        ByteArrayReader reader = new ByteArrayReader(bytes);
        String result = BitcoinSerializerUtils.deserializeVarStr(reader);
        return result;
    }

    /* Given a Key, it extracts the Tx Hash from it as long as the fullKey contains a Tx_hash, otherwise it returns null */
    default Optional<String> extractTxHashFromKey(byte[] key) {
        if (key == null || key.length == 0) return Optional.empty();
        Optional<String> result = Optional.empty();
        String keyStr = new String(key);
        if (keyStr.contains(KEY_PREFFIX_TX))
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX) + KEY_PREFFIX_TX.length(),
                    keyStr.lastIndexOf(KEY_SEPARATOR)));
        if (keyStr.contains(KEY_PREFFIX_TX_PROP)) {
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX_PROP) + KEY_PREFFIX_TX_PROP.length(),
                    keyStr.indexOf(KEY_SEPARATOR)));
        }
        if (keyStr.contains(KEY_PREFFIX_TX_LINK)) {
            // The Key is like: "tx_link:[txIndex]:[tx_hash]:
            // We extract the part "[txIndex]:[tx_hash]...
            String subStr = keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX_LINK) + KEY_PREFFIX_TX_LINK.length(), keyStr.lastIndexOf(KEY_SEPARATOR));
            // We extract the "[tx_hash]...
            result = Optional.of(subStr.substring(subStr.indexOf(KEY_SEPARATOR) + 1));
        }
        if (keyStr.contains(KEY_PREFFIX_TX_BLOCK)) {
            String keyStrAfterSeparator = keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX_BLOCK) + KEY_PREFFIX_TX_BLOCK.length());
            result = Optional.of(keyStr.substring(0, keyStrAfterSeparator.lastIndexOf(KEY_SEPARATOR)));
        }

        return result;
    }

    /* Given a Key, it extracts the Tx Hash from it as long as the fullKey contains a Tx_hash, otherwise it returns null */
    default Optional<String> extractBlockHashFromKey(byte[] key) {
        if (key == null || key.length == 0) return Optional.empty();
        Optional<String> result = Optional.empty();
        String keyStr = new String(key);

        if (keyStr.contains(KEY_PREFFIX_BLOCK))
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_BLOCK) + KEY_PREFFIX_BLOCK.length(),
                    keyStr.lastIndexOf(KEY_SEPARATOR)));
        else if (keyStr.contains(KEY_PREFFIX_BLOCK_PROP)) {
            String subKey = keyStr.substring(keyStr.indexOf(KEY_PREFFIX_BLOCK_PROP) + KEY_PREFFIX_BLOCK_PROP.length());
            result = Optional.of(subKey.substring(0, subKey.indexOf(KEY_SEPARATOR)));
        }
        else if (keyStr.contains(KEY_PREFFIX_TX_BLOCK)) {
            String keyStrAfterSeparator = keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX_BLOCK) + KEY_PREFFIX_TX_BLOCK.length());
            result = Optional.of(keyStrAfterSeparator.substring(keyStrAfterSeparator.indexOf(KEY_SEPARATOR) + KEY_SEPARATOR.length(),
                    keyStrAfterSeparator.lastIndexOf(KEY_SEPARATOR)));
        }
        else if(keyStr.contains(KEY_PREFFIX_ORPHAN_HASH)) {
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_ORPHAN_HASH) + KEY_PREFFIX_ORPHAN_HASH.length(),
                    keyStr.lastIndexOf(KEY_SEPARATOR)));
        }

        return result;
    }


    /**
     * It iterates over a series of Keys in the DB, and performs operations over them.
     *
     * @param iterator                      Iterator used to iterate over the Keys. This iterator is already configured to
     *                                      start iterating at a specific position (keyPreffix).
     * @param startingKeyIndex              Index to move the iterator to before starting. If ZERO, the iterator starts at
     *                                      the KeyPreffix its configured at, fi ONE it moves one position before starting, etc.
     * @param maxKeysToProcess              If specified, te iterator stops when this number of Keys have been reached.
     * @param taskForKey                    If specified, this task is executed per each Key the iterator finds.
     *                                      It takes 2 parameters:
     *                                          - 1: A Transaction Object (defined as object here, it needs to be cast to a
     *                                              concrete class, or might not be used, depending on DB implementation
     *                                          - 2: The Key on each iteration
     * @param taskForAllKeys                If specified this task is executed ONCE, after iterating over all the Keys. It takes
     *                                      all the kys as a parameters, so you must make sure this task is only specified when
     *                                      the number of keys is gonna be manageable.
     *                                      It takes 2 parameters:
     *                                          - 1: A Transaction Object (defined as object here, it needs to be cast to a
     *                                              concrete class, or might not be used, depending on DB implementation
     *                                          - 2: The List of ALL the keys processed
     */
    default void loopOverKeysAndRun(KeyValueIterator<byte[], T> iterator,
                                    Long startingKeyIndex,
                                    Optional<Long> maxKeysToProcess,
                                    BiConsumer<T, byte[]> taskForKey,
                                    BiConsumer<T, List<byte[]>> taskForAllKeys) {

        // We store each one of the Keys we process, so we can also trigger the global "taskForAllKeys", passing
        // all of them as a parameter. This list could be potentially huge, so we only use it if the
        // "taskForAllKeys" parameters has been set (not null)
        List<byte[]> allKeysToProcess = new ArrayList<>();

        long currentIndex = -1;
        long numKeysProcessed = 0;
        while(true) {
            if (!iterator.hasNext()) break;

            // We advance the Iterator...
            byte[] key = iterator.next();
            currentIndex++;

            // Exit Conditions:
            if (maxKeysToProcess.isPresent() && (numKeysProcessed >= maxKeysToProcess.get())) break;
            if (currentIndex < startingKeyIndex) continue;

            // If we reach this far then it means that we have to process it, so we do it and move forward:
            taskForKey.accept(iterator.getCurrentTransaction(), key);
            numKeysProcessed++;

            // We only use this list if the global task has been defined:
            if (taskForAllKeys != null) allKeysToProcess.add(key);
        }

        // We are done iterating over the Keys. We trigger the global Task:
        if (taskForAllKeys != null) taskForAllKeys.accept(iterator.getCurrentTransaction(), allKeysToProcess);
    }

    /** Convenience method */
    default void loopOverKeysAndRun(KeyValueIterator<byte[], T> iterator,
                                    BiConsumer<T, byte[]> taskForKey,
                                    BiConsumer<T, List<byte[]>> taskForAllKeys) {
        loopOverKeysAndRun(iterator, 0L, Optional.empty(), taskForKey, taskForAllKeys);
    }

    /* Transaction Management: */

    default void executeInTransaction(T tr, Runnable task) {
        try {
            task.run();
            commitTransaction(tr);
        } catch (Exception e) {
            e.printStackTrace();
            rollbackTransaction(tr);
            throw new RuntimeException();
        }
    }

    /*
        Block Store DB Operations:
        These methods execute the business logic. Most of them map a method of the BlockStore interface, but with
        some limitations:
         - They do NOT trigger Events
         - They do NOT create new DB Transaction, instead they need to reuse one passed as a parameter.
     */

    default List<HeaderReadOnly> _saveBlock(T tr, HeaderReadOnly blockHeader){
        ArrayList<HeaderReadOnly> savedBlocks = new ArrayList<>();
        String blockHash = blockHeader.getHash().toString();

        if(!isBytesOk(_getBlockBytes(tr, blockHash))) {
            save(tr, fullKeyForBlock(tr, blockHash), bytes(blockHeader));
            savedBlocks.add(blockHeader);
        }

        return savedBlocks;
    }

    default List<HeaderReadOnly> _saveBlocks(T tr, List<HeaderReadOnly> blockHeaders) {
        List<HeaderReadOnly> savedBlocks = new ArrayList<>();
        blockHeaders.forEach(b -> {
            List<HeaderReadOnly> blocksSaved = _saveBlock(tr, b);
            if (!blocksSaved.isEmpty()) {
                savedBlocks.addAll(blocksSaved);
            }
        });

        return savedBlocks;
    }

    default void _removeBlock(T tr, String blockHash) {
        remove(tr, fullKeyForBlock(tr, blockHash));
        remove(tr, fullKeyForBlockNumTxs(tr, blockHash));
        remove(tr, fullKeyForBlockTxIndex(tr, blockHash));

        //If the block is an orphan, remove it
        _removeOrphanBlockHash(tr, blockHash);

        // IF a metadata class has been defined, we remove it too
        if (getMetadataClassForBlocks() != null) {
            _removeBlockMetadata(tr, blockHash);
        }
    }

    default void _removeBlocks(T tr, List<String> blockHashes) {
        blockHashes.forEach(h -> {
            _removeBlock(tr, h);
            _unlinkBlock(h);
        });
    }

    default void _saveOrphanBlockHash(T tr, String blockHash) {
        byte[] key = fullKeyForOrphanBlockHash(tr, blockHash);
        save(tr, key, new byte[0]);
        getLogger().trace("Orphan Block Saved/Updated [block: " + blockHash + "]");
    }

    default void _removeOrphanBlockHash(T tr, String blockHash) {
        remove(tr, fullKeyForOrphanBlockHash(tr, blockHash));
    }


    default byte[] _getBlockBytes(T tr, String blockHash) {
        return read(tr, fullKeyForBlock(tr, blockHash));
    }

    default HeaderReadOnly _getBlock(T tr, String blockHash) {
        // We recover the Whole Block Header:
        HeaderReadOnly result = toBlockHeader(_getBlockBytes(tr, blockHash));
        if (result == null ) return null;

        return result;
    }

    private Metadata _getBlockMetadata(T tr, String blockHash) {
        try {
            Metadata result = null;
            byte[] key = fullKeyForBlockMetadata(tr, blockHash);
            byte[] value = read(tr, key);
            if (value != null) {
                result = (Metadata) getMetadataClassForBlocks().getConstructor().newInstance();
                result.load(value);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Metadata _getTxMetadata(T tr, String txHash) {
        try {
            Metadata result = null;
            byte[] key = fullKeyForTxMetadata(tr, txHash);
            byte[] value = read(tr, key);
            if (value != null) {
                result = (Metadata) getMetadataClassForTxs().getConstructor().newInstance();
                result.load(value);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void _saveBlockMetadata(T tr, String blockHash, Metadata metadata) {
        try {
            byte[] key = fullKeyForBlockMetadata(tr, blockHash);
            byte[] value = metadata.serialize();
            save(tr, key, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void _saveTxMetadata(T tr, String txHash, Metadata metadata) {
        try {
            byte[] key = fullKeyForTxMetadata(tr, txHash);
            byte[] value = metadata.serialize();
            save(tr, key, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void _removeBlockMetadata(T tr, String blockHash) {
        try {
            byte[] key = fullKeyForBlockMetadata(tr, blockHash);
            remove(tr, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void _removeTxMetadata(T tr, String txHash) {
        try {
            byte[] key = fullKeyForTxMetadata(tr, txHash);
            remove(tr, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    default void _saveTx(T tr, Tx tx) {
        // We store the Whole TX Object
        String txHash = tx.getHash().toString();
        save(tr, fullKeyForTx(tr, txHash), bytes(tx));
    }

    default void _saveTxs(T tr, List<Tx> txs) {
        txs.forEach(tx -> _saveTx(tr, tx));
    }

    default byte[] _getTxBytes(T tr, String txHash) {
        return read(tr, fullKeyForTx(tr, txHash));
    }

    default Tx _getTx(T tr, String txHash) {
        return toTx(_getTxBytes(tr, txHash));
    }

    default void _removeTx(T tr, String txHash) {
        remove(tr, fullKeyForTx(tr, txHash));
        List<String> blockHashes =_getBlockHashesLinkedToTx(tr, txHash);

        for (String blockHash : blockHashes)
            remove(tr, fullKeyForTxBlock(tr, txHash, blockHash));

        // IF a metadata class has been defined, we remove it too
        if (getMetadataClassForBlocks() != null) {
            _removeTxMetadata(tr, txHash);
        }
    }

    default void _removeTxs(T tr, List<String> txHashes) {
        txHashes.forEach(h -> _removeTx(tr, h));
    }

    default List<String> _getBlockHashesLinkedToTx(T tr, String txHash) {
        List<String> result = new ArrayList<>();
        byte[] preffix = fullKey(fullKeyForTxs(), KEY_PREFFIX_TX_BLOCK  + txHash + KEY_SEPARATOR);
        String preffixStr = new String(preffix);
        Iterator<String> it = getIterator(tr, preffix, null, null, e -> extractBlockHashFromKey(keyFromItem(e)).get());
        while (it.hasNext()) result.add(it.next());
        return result;
    }

    default Long _getBlockNumTxs(T tr, String blockHash) {
        byte[] key = fullKeyForBlockNumTxs(tr, blockHash);
        String temp = new String(key);
        Long numKeys = toLong(read(tr, key));
        return (numKeys != null) ? numKeys : 0;
    }

    default void _addBlockNumTxs(T tr, String blockHash, long numTxsToAdd) {
        byte[] keyBlockNumTxs = fullKeyForBlockNumTxs(tr, blockHash);
        byte[] numTxsValue = read(tr, keyBlockNumTxs);
        long numTxsLong = (numTxsValue != null) ? (toLong(numTxsValue) + numTxsToAdd) : numTxsToAdd;
        save(tr, keyBlockNumTxs, bytes(numTxsLong));
    }

    default long _getTxIndexForBlock(T tr, String blockHash) {
        byte[] key = fullKeyForBlockTxIndex(tr, blockHash);
        byte[] value = read(tr, key);
        long result = (value != null)? toLong(value) : 0;
        return result;
    }

    default Optional<Long> _getTxIndexForTxBlock(T tr, String txHash, String blockHash) {
        byte[] key = fullKeyForTxBlock(tr, txHash, blockHash);
        byte[] value = read(tr, key);
        Optional<Long> result = (value != null)? Optional.of(toLong(value)) : Optional.empty();
        return result;
    }

    default void _addTxIndexToBlock(T tr, String blockHash, long indexToAdd) {
        long newValue = _getTxIndexForBlock(tr, blockHash) + indexToAdd;
        byte[] key = fullKeyForBlockTxIndex(tr, blockHash);
        save(tr, key, bytes(newValue));
    }

    default void _linkTxToBlock(T tr, String txHash, String blockHash, byte[] blockDirFullKey, long txIndex) {
        // We add a Key in this Block subfolder for this Tx:
        save(tr, fullKeyForBlockTx(tr, blockDirFullKey, txHash, txIndex), bytes(1L)); // the value is NOT important here...

        // We add a Key in the "TXs" folder
        save(tr, fullKeyForTxBlock(tr, txHash, blockHash), bytes(txIndex));
    }

    default void _linkTxToBlock(T tr, String txHash, String blockHash, long txIndex) {
        byte[] blockDirFullKey = fullKeyForBlockDir(tr, blockHash);
        _linkTxToBlock(tr, txHash, blockHash, blockDirFullKey, txIndex);
    }

    default void _unlinkTxFromBlock(T tr, String txHash, String blockHash, long txIndex) {
        byte[] blockDirFullKey = fullKeyForBlockDir(tr, blockHash);

        // We remove a Key from this Block subfolder:
        remove(tr, fullKeyForBlockTx(tr, blockDirFullKey, txHash, txIndex));

        // We remove the Key from the "Txs" folder:
        remove(tr, fullKeyForTxBlock(tr, txHash, blockHash));

        // There is also a Property where we save the number of Txs belonging to this Block. We update the Value:
        _addBlockNumTxs(tr, blockHash, -1);
    }

    default void _unlinkTx(T tr, String txHash) {

        // We get the list of Block Hashes linked to this Tx:
        List<String> blocksLinked = _getBlockHashesLinkedToTx(tr, txHash);

        // For each Block hash, we get the index of this Tx within that Block, and we unlink it:
        for (String blockHash : blocksLinked) {
            Optional<Long> txIndex = _getTxIndexForTxBlock(tr, txHash, blockHash);
            if (txIndex.isPresent())
                _unlinkTxFromBlock(tr, txHash, blockHash, txIndex.get());
        }
    }

    default void _unlinkBlock(String blockHash) {
        // We locate the Block Subfolder and the Key to start iterating over:
        byte[] keyStart = fullKey(fullKeyForBlockDir(blockHash), null);

        // For each Tx in this Block, we remove the "tx_block" property in the "TXs" directory:
        KeyValueIterator<byte[], T> iterator = getIterator(keyStart, null, null, this::keyFromItem);
        loopOverKeysAndRun(iterator, (tr, key) -> {
            String txHash = extractTxHashFromKey(key).get();
            remove(tr, fullKeyForTxBlock(tr, txHash, blockHash));
        }, null);

        // Now we remove the whole Block directory:
        removeBlockDir(blockHash);
    }

    default boolean _isTxLinkToBlock(T tr, String txHash, String blockHash) {
        List<String> blocksLinked = _getBlockHashesLinkedToTx(tr, txHash);
        return blocksLinked.contains(blockHash);
    }

    default void _removeBlockTxs(String blockHash, Consumer<String> txHashConsumer) {

        // We update now the numTxs and txIndex properties of this block:
        T transaction = createTransaction();
        executeInTransaction(transaction, () -> {
            long numTxs = _getBlockNumTxs(transaction, blockHash);
            long txIndex = _getTxIndexForBlock(transaction, blockHash);
            _addBlockNumTxs(transaction, blockHash, -numTxs);
            _addTxIndexToBlock(transaction, blockHash, -txIndex);
        });

        // We locate the Block Subfolder and the Key to start iterating over:
        byte[] keyStart = fullKey(fullKeyForBlockDir(blockHash), null);

        // For each Tx in this Block, we update its "blocks" property, removing the reference to this Block.
        KeyValueIterator<byte[], T> iterator = getIterator(keyStart, null, null, e -> keyFromItem(e));
        loopOverKeysAndRun(iterator, (tr, key) -> {
            String txHash = extractTxHashFromKey(key).get();
            // We remove the "tx_block" Key, where we store the txIndex for this Tx/Block:
            remove(tr, fullKeyForTxBlock(tr, txHash, blockHash));
            // We check the blocks linked to this Tx. If there are no more blocks linked, we also remove the Tx itself:
            List<String> blockHashes =_getBlockHashesLinkedToTx(tr, txHash);
            if (blockHashes.isEmpty()) {
                remove(tr, fullKeyForTx(tr, txHash));
            }
            txHashConsumer.accept(txHash);
        }, null);

        // Now we remove the whole Block directory:
        removeBlockDir(blockHash);
    }

    /* DB High-Level Operations: */

    @Override
    default List<HeaderReadOnly> saveBlock(HeaderReadOnly blockHeader) {
        try {
            getLock().writeLock().lock();

            AtomicReference<List<HeaderReadOnly>> atomicArray = new AtomicReference(new ArrayList<>());

            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                atomicArray.set(_saveBlock(tr, blockHeader));
                _triggerBlocksStoredEvent(atomicArray.get());
            });

            return atomicArray.get();

        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default List<HeaderReadOnly> saveBlocks(List<HeaderReadOnly> blockHeaders) {

        try {
            getLock().writeLock().lock();

            AtomicReference<List<HeaderReadOnly>> atomicArray = new AtomicReference(new ArrayList<>());
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
            */
            List<List<HeaderReadOnly>> subLists = Lists.partition(blockHeaders, getConfig().getTransactionBatchSize());
            for (List<HeaderReadOnly> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> {
                    atomicArray.set(_saveBlocks(tr, subList));
                    _triggerBlocksStoredEvent(atomicArray.get());
                });
            }

            return atomicArray.get();

        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default boolean containsBlock(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            AtomicBoolean result = new AtomicBoolean();
            T tr = createTransaction();
            executeInTransaction(tr, () -> result.set(_getBlockBytes(tr, blockHash.toString()) != null));
            return result.get();
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Optional<HeaderReadOnly> getBlock(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            AtomicReference<HeaderReadOnly> result = new AtomicReference<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> result.set(_getBlock(tr, blockHash.toString())));
            return Optional.ofNullable(result.get());
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Iterator<Sha256Hash> getBlocksIterator() {
        byte[] keyPreffix = fullKey(fullKeyForBlocks(), KEY_PREFFIX_BLOCK);
        // The "buildItemBy" is the function used to take a Key and return each Item of the Iterator. The iterator
        // will returns a series of BlockHeader, so this function will build a Block Hash out of a Key:

        Function<E, Sha256Hash> buildItemBy = (E item) -> {
            byte[] key = keyFromItem(item);
            String blockHash = extractBlockHashFromKey(key).get();
            return Sha256Hash.wrap(blockHash);
        };

        // With everything set up, we create our Iterator and return it wrapped up in an Iterable:
        Iterator<Sha256Hash> iterator = getIterator(keyPreffix, null, null, buildItemBy);
        return iterator;
    }

    @Override
    default void removeBlock(Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
               executeInTransaction(tr, () -> {
                   _removeBlock(tr, blockHash.toString());
                   _unlinkBlock(blockHash.toString());
                   _triggerBlocksRemovedEvent(Arrays.asList(blockHash));
              });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void removeBlocks(List<Sha256Hash> blockHashes) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */

            List<List<Sha256Hash>> subLists = Lists.partition(blockHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Hash> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> _removeBlocks(tr, subList.stream().map(h -> h.toString()).collect(Collectors.toList())));
            }
            _triggerBlocksRemovedEvent(blockHashes);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default long getNumBlocks() {
        try {
            getLock().readLock().lock();
            byte[] startingWith = fullKey(fullKeyForBlocks(), KEY_PREFFIX_BLOCK);
            return numKeys(startingWith);
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Optional<Metadata> getBlockMetadata(Sha256Hash blockHash) {
        AtomicReference<Metadata> result = new AtomicReference<>();
        try {
            getLock().readLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                Metadata metadata = _getBlockMetadata(tr, blockHash.toString());
                result.set(metadata);
            });
        } finally {
            getLock().readLock().unlock();
        }
        return Optional.ofNullable(result.get());
    }

    @Override
    default Optional<Metadata> getTxMetadata(Sha256Hash txHash) {
        AtomicReference<Metadata> result = new AtomicReference<>();
        try {
            getLock().readLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                Metadata metadata = _getTxMetadata(tr, txHash.toString());
                result.set(metadata);
            });
        } finally {
            getLock().readLock().unlock();
        }
        return Optional.ofNullable(result.get());
    }

    @Override
    default void saveBlockMetadata(Sha256Hash blockHash, Metadata metadata) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                _saveBlockMetadata(tr, blockHash.toString(), metadata);
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void saveTxMetadata(Sha256Hash txHash, Metadata metadata) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> _saveTxMetadata(tr, txHash.toString(), metadata));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void removeBlockMetadata(Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                _removeBlockMetadata(tr, blockHash.toString());
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void removeTxMetadata(Sha256Hash txHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                _removeTxMetadata(tr, txHash.toString());
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void saveTx(Tx tx) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                _saveTx(tr, tx);
                _triggerTxsStoredEvent(Arrays.asList(tx));
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void saveTxs(List<Tx> txs) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            List<List<Tx>> subLists = Lists.partition(txs, getConfig().getTransactionBatchSize());
            for (List<Tx> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> _saveTxs(tr, subList));
            }
            _triggerTxsStoredEvent(txs);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default boolean containsTx(Sha256Hash txHash) {
        try {
            getLock().readLock().lock();
            AtomicBoolean result = new AtomicBoolean();
            T tr = createTransaction();
            executeInTransaction(tr, () -> result.set(_getTxBytes(tr, txHash.toString()) != null));
            return result.get();
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Optional<Tx> getTx(Sha256Hash txHash) {
        try {
            getLock().readLock().lock();
            AtomicReference<Tx> result = new AtomicReference<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> result.set(_getTx(tr, txHash.toString())));
            return Optional.ofNullable(result.get());
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default void removeTx(Sha256Hash txHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                _removeTx(tr, txHash.toString());
                _triggerTxsRemovedEvent(Arrays.asList(txHash));
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void removeTxs(List<Sha256Hash> txHashes) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            List<List<Sha256Hash>> subLists = Lists.partition(txHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Hash> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () ->
                        _removeTxs(tr, subList.stream().map(h -> h.toString()).collect(Collectors.toList()))
                );
            }
            _triggerTxsRemovedEvent(txHashes);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default List<Sha256Hash> getPreviousTxs(Sha256Hash txHash) {
        try {
            getLock().readLock().lock();
            List<Sha256Hash> result = new ArrayList<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                Tx tx = _getTx(tr, txHash.toString());
                if (tx == null) return;
                Set<Sha256Hash> txsNeeded = tx.getInputs().stream()
                        .map(i -> i.getOutpoint().getHash())
                        .collect(Collectors.toSet());
                result.addAll(txsNeeded);
            });
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default long getNumTxs() {
        try {
            getLock().readLock().lock();
            byte[] startingWith = fullKey(fullKeyForTxs(), KEY_PREFFIX_TX);
            return numKeys(startingWith);
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default List<Tx> saveTxsIfNotExist(List<Tx> txs) {

        try {
            getLock().writeLock().lock();
            List<Tx> result = new ArrayList<>();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            List<List<Tx>> subLists = Lists.partition(txs, getConfig().getTransactionBatchSize());
            for (List<Tx> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> {
                    List<Tx> partialTxs = _saveTxsIfNotExist(tr, subList);
                    result.addAll(partialTxs);
                    }
                );
            }
            return result;
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default CompletableFuture<List<Tx>> saveTxsIfNotExistAsync(List<Tx> txs) {
        CompletableFuture<List<Tx>> result = new CompletableFuture<>();
        getExecutor().submit(() -> {
            List<Tx> txsInserted = saveTxsIfNotExist(txs);
            result.complete(txsInserted);
        });
        return result;
    }

    @Override
    default void linkTxToBlock(Sha256Hash txHash, Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                    // We get the Tx Index for this block:
                    long txIndex = _getTxIndexForBlock(tr, blockHash.toString());
                    // we remove link the Tx to the block:
                     _linkTxToBlock(tr, txHash.toString(), blockHash.toString(), txIndex);
                     // We update the Tx Index for this block, so the next Tx linked to it will use an (index +1)
                    _addTxIndexToBlock(tr, blockHash.toString(), 1);
                    // We update the number of Txs contained in this block:
                    _addBlockNumTxs(tr, blockHash.toString(), 1);
                }
            );
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void linkTxsToBlock(List<Sha256Hash> txsHashes, Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            byte[] blockDirFullKey = fullKeyForBlockDir(blockHash.toString());
            List<List<Sha256Hash>> subLists = Lists.partition(txsHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Hash> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> {
                        // We get the Tx Index for this Block:
                        long txIndex = _getTxIndexForBlock(tr, blockHash.toString());

                        // We iterate over the Txs and we link them using a different index each:
                        for (Sha256Hash txHash : subList) {
                            _linkTxToBlock(tr, txHash.toString(), blockHash.toString(), blockDirFullKey, txIndex++);
                        }

                        // we update the number of Txs of this block:
                        _addBlockNumTxs(tr, blockHash.toString(), subList.size());

                        // We update the TxIndex for this Block:
                        _addTxIndexToBlock(tr, blockHash.toString(), subList.size());
                    }
                );
            }
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkTxFromBlock(Sha256Hash txHash, Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                // We get the Index fo this Tx within this Block:
                Optional<Long> txIndex = _getTxIndexForTxBlock(tr, txHash.toString(), blockHash.toString());
                // We unlink it:
                if (txIndex.isPresent())
                    _unlinkTxFromBlock(tr, txHash.toString(), blockHash.toString(), txIndex.get());
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkTxsFromBlock(List<Sha256Hash> txsHashes, Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            List<List<Sha256Hash>> subLists = Lists.partition(txsHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Hash> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> {
                    // We get the Tx Index for this Block:
                    long txIndex = _getTxIndexForBlock(tr, blockHash.toString());

                    // We iterate over the Txs and we link them using a different index each:
                    for (Sha256Hash txHash : subList) {
                        _unlinkTxFromBlock(tr, txHash.toString(), blockHash.toString(), txIndex++);
                    }

                    // we update the number of Txs of this block:
                    _addBlockNumTxs(tr, blockHash.toString(), -subList.size());

                    // We update the TxIndex for this Block:
                    _addTxIndexToBlock(tr, blockHash.toString(), -subList.size());

                    });
            }
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkTx(Sha256Hash txHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> _unlinkTx(tr, txHash.toString()));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkBlock(Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();
            _unlinkBlock(blockHash.toString());
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default boolean isTxLinkToblock(Sha256Hash txHash, Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            AtomicBoolean result = new AtomicBoolean();
            T tr = createTransaction();
            executeInTransaction(tr, () -> result.set(_isTxLinkToBlock(tr, txHash.toString(), blockHash.toString())));
            return result.get();
        } finally {
            getLock().readLock().unlock();
        }

    }

    @Override
    default List<Sha256Hash> getBlockHashLinkedToTx(Sha256Hash txHash) {
        try {
            getLock().readLock().lock();
            List<Sha256Hash> result = new ArrayList<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                List<String> hashes = _getBlockHashesLinkedToTx(tr, txHash.toString());
                result.addAll(hashes.stream().map(Sha256Hash::wrap).collect(Collectors.toList()));

            });
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Iterable<Sha256Hash> getBlockTxs(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();

            byte[] keyPreffix = fullKey(fullKeyForBlockDir(blockHash.toString()), KEY_PREFFIX_TX_LINK);
            Function<E, Sha256Hash> buildKeyFunction = e -> {
                byte[] key = keyFromItem(e);
                return Sha256Hash.wrap(extractTxHashFromKey(key).get());
            };

            Iterator<Sha256Hash> it = getIterator(keyPreffix, null, null, buildKeyFunction);
            Iterable<Sha256Hash> result = () -> it;
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default long getBlockNumTxs(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            AtomicLong result = new AtomicLong();
            T tr = createTransaction();
            executeInTransaction(tr, () -> result.set(_getBlockNumTxs(tr, blockHash.toString())));
            return result.get();
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default void saveBlockTxs(Sha256Hash blockHash, List<Tx> txs) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            // In this case, since in each Transaction we are NOt only Linking the Txs but ALSO saving the TX THEMSELVES,
            // we are using a TR Batch Size Twice as SMALL as usual...
            List<List<Tx>> subLists = Lists.partition(txs, getConfig().getTransactionBatchSize() / 2);
            byte[] blockDirFullKey = fullKeyForBlockDir(blockHash.toString());
            for (List<Tx> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> {
                    // We store the TX...
                    _saveTxs(tr, subList);
                    // Now we link them:
                    // We get the Tx Index for this Block:
                    long txIndex = _getTxIndexForBlock(tr, blockHash.toString());

                    // We iterate over the Txs and we link them using a different index each:
                    for (Tx tx : subList) {
                        _linkTxToBlock(tr, tx.getHash().toString(), blockHash.toString(), blockDirFullKey, txIndex++);
                    }

                    // we update the number of Txs of this block:
                    _addBlockNumTxs(tr, blockHash.toString(), subList.size());

                    // We update the TxIndex for this Block:
                    _addTxIndexToBlock(tr, blockHash.toString(), subList.size());

                });
            } // for...
            _triggerTxsStoredEvent(txs);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void removeBlockTxs(Sha256Hash blockHash) {
        try {
            getLock().writeLock().lock();

                // We remove all The Txs from this Block. the problem is that we need to trigger an TXS_REMOVED Event but the
                // number of Txs must be huge, so we cannot just trigger a single event with a huge list inside. Instead, we
                // are keeping track of the number of Txs we remove, and we only trigger an event when we reach the Threshold.
                // We put all that logic inside a Lambda function that will be passed to the method that removes the Txs...

                List<Sha256Hash> batchTxsRemoved = new ArrayList<>();
                Consumer<String> txHashConsumer = txHash -> {
                    batchTxsRemoved.add(Sha256Hash.wrap(txHash));
                    if (batchTxsRemoved.size() == MAX_EVENT_ITEMS) {
                        _triggerTxsRemovedEvent(batchTxsRemoved);
                        batchTxsRemoved.clear();
                    }
                };
                _removeBlockTxs(blockHash.toString(), txHashConsumer);

                // In case there are still Tx that have not been published in an Event...
                if (batchTxsRemoved.size() > 0) _triggerTxsRemovedEvent(batchTxsRemoved);
          //  }

        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default Optional<BlocksCompareResult> compareBlocks(Sha256Hash blockHashA, Sha256Hash blockHashB) {
        try {
            getLock().readLock().lock();
            Optional<HeaderReadOnly> blockHeaderA = getBlock(blockHashA);
            Optional<HeaderReadOnly> blockHeaderB = getBlock(blockHashB);

            if (blockHeaderA.isEmpty() || blockHeaderB.isEmpty()) return Optional.empty();

            BlocksCompareResult.BlocksCompareResultBuilder resultBuilder = BlocksCompareResult.builder()
                    .blockA(blockHeaderA.get())
                    .blockB(blockHeaderB.get());

            byte[] keyPreffixA = fullKey(fullKeyForBlockDir(blockHashA.toString()), KEY_PREFFIX_TX_LINK);
            byte[] keyPreffixB = fullKey(fullKeyForBlockDir(blockHashB.toString()), KEY_PREFFIX_TX_LINK);

            // We create an Iterable for the TXs in common:
            Function<E, Sha256Hash> buildItemBy = e -> Sha256Hash.wrap(extractTxHashFromKey(keyFromItem(e)).get());

            BiPredicate<T, byte[]> commonKeyValid = (tr, k) -> _isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashB.toString());
            Iterator<Sha256Hash> commonIterator = getIterator(keyPreffixA, null, commonKeyValid, buildItemBy);
            Iterable<Sha256Hash> commonIterable = () -> commonIterator;
            resultBuilder.txsInCommonIt(commonIterable);

            // We create an Iterable for the TXs that are ONLY in the block A:

            BiPredicate<T, byte[]> onlyAKeyValid = (tr, k) -> !_isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashB.toString());
            Iterator<Sha256Hash> onlyAIterator = getIterator(keyPreffixA, null, onlyAKeyValid, buildItemBy);
            Iterable<Sha256Hash> onlyAIterable = () -> onlyAIterator;
            resultBuilder.txsOnlyInA(onlyAIterable);

            // We create an Iterable for the TXs that are ONLY in the block B:
            BiPredicate<T, byte[]> onlyBKeyValid = (tr, k) -> !_isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashA.toString());
            Iterator<Sha256Hash> onlyBIterator = getIterator(keyPreffixB, null, onlyBKeyValid, buildItemBy);
            Iterable<Sha256Hash> onlyBIterable = () -> onlyBIterator;
            resultBuilder.txsOnlyInB(onlyBIterable);

            return Optional.of(resultBuilder.build());
        } finally {
            getLock().readLock().unlock();
        }
    }

    /* Events triggering Operations */

    default void _triggerBlocksStoredEvent(List<HeaderReadOnly> blockHeaders) {
        if (isTriggerBlockEvents()) {
            List<Sha256Hash> blockHashes = blockHeaders.stream().map(b -> b.getHash()).collect(Collectors.toList());
            getEventBus().publish(new BlocksSavedEvent(blockHashes));
        }
    }

    default void _triggerBlocksRemovedEvent(List<Sha256Hash> blockHashes) {
        if (isTriggerBlockEvents())
            getEventBus().publish(new BlocksRemovedEvent(blockHashes));
    }

    default void _triggerTxsStoredEvent(List<Tx> txs) {
        if (isTriggerTxEvents()) {
            List<Sha256Hash> txHashes = txs.stream().map(tx -> tx.getHash()).collect(Collectors.toList());
            getEventBus().publish(new TxsSavedEvent(txHashes));
        }
    }

    default void _triggerTxsRemovedEvent(List<Sha256Hash> txHashes) {
        if (isTriggerTxEvents())
            getEventBus().publish(new TxsRemovedEvent(txHashes));
    }


}
