package com.nchain.jcl.store.keyValue.blockStore;


import com.google.common.collect.Lists;
import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.serialization.BlockHeaderSerializer;
import com.nchain.jcl.base.serialization.TxSerializer;
import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.store.blockStore.BlockStore;
import com.nchain.jcl.store.blockStore.BlocksCompareResult;
import com.nchain.jcl.store.blockStore.events.*;
import com.nchain.jcl.store.keyValue.common.HashesList;
import com.nchain.jcl.store.keyValue.common.HashesListSerializer;
import com.nchain.jcl.store.keyValue.common.KeyValueIterator;
import org.slf4j.Logger;

import java.util.*;
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
 *  - The second one is store din the "/Txs" directory, using a "tx_block:[txHash]:[blockHash:" Key. In the previous
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

    /** Definition of the Directory structure: */
    String DIR_BLOCKCHAIN            = "blockchain";
    String DIR_BLOCKS                = "blocks";
    String DIR_TXS                   = "txs";

    /** preffixes/suffixes used in Keys: */
    String KEY_SEPARATOR             = ":";
    String KEY_PREFFIX_BLOCK         = "block" + KEY_SEPARATOR;       // A Whole Block
    String KEY_PREFFIX_BLOCK_PROP    = "block_p" + KEY_SEPARATOR;     // Property suffix
    String KEY_PREFFIX_TX            = "tx" + KEY_SEPARATOR;          // A whole Tx
    String KEY_PREFFIX_TX_PROP       = "tx_p" + KEY_SEPARATOR;        // Property suffix
    String KEY_PREFFIX_TX_LINK       = "tx_link" + KEY_SEPARATOR;     // A Key that represents a reference to a Tx
    String KEY_SUFFIX_BLOCK_NUMTXS   = KEY_SEPARATOR + "numTxs" + KEY_SEPARATOR;      // Property suffix: the number of Txs in a Block
    String KEY_PREFFIX_TX_BLOCK      = "tx_block_link" + KEY_SEPARATOR;               // Property suffix: The list of blocks this Tx is linked to


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

    /*
     * Functions to generate FULL Keys. A FULL fullKey can be used to Insert/Read/Remove an item from the DB, and it
     * represents a Whole Path, using the hierarchy described (see class javadoc). They take a "tr" (Transaction)
     * because some specific DB might need to perform DB operations in order to create or access specific parts of
     * this Path.
     */

    byte[] fullKeyForBlocks(T tr);
    byte[] fullKeyForBlock(T tr, String blockHash);
    byte[] fullKeyForBlockNumTxs(T tr, String blockHash);
    byte[] fullKeyForBlockTx(T tr, String blockHash, String txHash);
    byte[] fullKeyForBlockTx(T tr, byte[] blockDirFullKey, String txHash);
    byte[] fullKeyForBlockDir(T tr, String blockHash);
    byte[] fullKeyForTxs(T tr);
    byte[] fullKeyForTx(T tr, String txHash);
    byte[] fullKeyForTxBlock(T tr, String txHash, String blockHash);
    byte[] fullKeyForBlocks();
    byte[] fullKeyForTxs();
    byte[] keyStartingWith(byte[] preffix); // Returns a Key that can be used for "startsWith" comparison
    byte[] fullKey(Object ...subKeys);      // Returns a FULL Key that is a concatenation of the subKeys provided
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
    default String keyForTx(String txHash)                          { return KEY_PREFFIX_TX + txHash + KEY_SEPARATOR;}
    default String keyForTxBlock(String txHash, String blockHash)   { return KEY_PREFFIX_TX_BLOCK + txHash + KEY_SEPARATOR + blockHash + KEY_SEPARATOR; }
    default String keyForBlockTx(String txHash)                     { return KEY_PREFFIX_TX_LINK + txHash + KEY_SEPARATOR; }
    default String keyForBlockDir(String blockHash)                 { return blockHash;}

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

    default byte[] bytes(HashesList chainTips)        { return HashesListSerializer.getInstance().serialize(chainTips); }
    default byte[] bytes(BlockHeader header)          { return BlockHeaderSerializer.getInstance().serialize(header);  }
    default byte[] bytes(Tx tx)                       { return TxSerializer.getInstance().serialize(tx); }
    default byte[] bytes(Long value)                  { return ByteTools.uint64ToByteArrayLE(value); }
    default byte[] bytes(Integer value)               { return ByteTools.uint32ToByteArrayLE(value); }


    /* Functions to deserialize Objects: */

    default boolean     isBytesOk(byte[] bytes)       { return (bytes != null && bytes.length > 0);}
    default BlockHeader toBlockHeader(byte[] bytes)   { return (isBytesOk(bytes)) ? BlockHeaderSerializer.getInstance().deserialize(bytes) : null;}
    default Tx          toTx(byte[] bytes)            { return (isBytesOk(bytes)) ? TxSerializer.getInstance().deserialize(bytes) : null;}
    default HashesList  toHashes(byte[] bytes)        { return (isBytesOk(bytes)) ? HashesListSerializer.getInstance().deserialize(bytes) : null;}
    default long        toLong(byte[] bytes)          { return (isBytesOk(bytes)) ? ByteTools.readInt64LE(bytes) : null;}
    default int         toInt(byte[] bytes)           { return (isBytesOk(bytes)) ? (int) ByteTools.readUint32(bytes) : null;}


    /* Given a Key, it extracts the Tx Hash from it as long as the fullKey contains a Tx_hash, otherwise it returns null */
    default Optional<String> extractTxHashFromKey(byte[] key) {
        if (key == null || key.length == 0) return Optional.empty();
        Optional<String> result = Optional.empty();
        String keyStr = new String(key);
        if (keyStr.contains(KEY_PREFFIX_TX))
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX) + KEY_PREFFIX_TX.length(),
                    keyStr.lastIndexOf(KEY_SEPARATOR)));
        if (keyStr.contains(KEY_PREFFIX_TX_PROP))
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX_PROP) + KEY_PREFFIX_TX_PROP.length(),
                    keyStr.lastIndexOf(KEY_SEPARATOR)));
        if (keyStr.contains(KEY_PREFFIX_TX_LINK))
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX_LINK) + KEY_PREFFIX_TX_LINK.length(),
                    keyStr.lastIndexOf(KEY_SEPARATOR)));
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
        if (keyStr.contains(KEY_PREFFIX_BLOCK_PROP))
            result = Optional.of(keyStr.substring(keyStr.indexOf(KEY_PREFFIX_BLOCK_PROP) + KEY_PREFFIX_BLOCK_PROP.length(),
                    keyStr.lastIndexOf(KEY_SEPARATOR)));
        if (keyStr.contains(KEY_PREFFIX_TX_BLOCK)) {
            String keyStrAfterSeparator = keyStr.substring(keyStr.indexOf(KEY_PREFFIX_TX_BLOCK) + KEY_PREFFIX_TX_BLOCK.length());
            result = Optional.of(keyStrAfterSeparator.substring(keyStrAfterSeparator.indexOf(KEY_SEPARATOR) + KEY_SEPARATOR.length(),
                    keyStrAfterSeparator.lastIndexOf(KEY_SEPARATOR)));
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

    default void _saveBlock(T tr, BlockHeader blockHeader) {
        String blockHash = blockHeader.getHash().toString();
        save(tr, fullKeyForBlock(tr, blockHash), bytes(blockHeader));
        save(tr, fullKeyForBlockNumTxs(tr, blockHash), bytes(blockHeader.getNumTxs()));
    }

    default void _saveBlocks(T tr, List<BlockHeader> blockHeaders) {
        blockHeaders.forEach(b -> _saveBlock(tr, b));
    }

    default void _removeBlock(T tr, String blockHash) {
        remove(tr, fullKeyForBlock(tr, blockHash));
        remove(tr, fullKeyForBlockNumTxs(tr, blockHash));
    }

    default void _removeBlocks(T tr, List<String> blockHashes) {
        blockHashes.forEach(h -> {
            _removeBlock(tr, h);
            _unlinkBlock(h);
        });
    }

    default byte[] _getBlockBytes(T tr, String blockHash) {
        return read(tr, fullKeyForBlock(tr, blockHash));
    }

    default BlockHeader _getBlock(T tr, String blockHash) {
        // We recover the Whole Block Header:
        BlockHeader result = toBlockHeader(_getBlockBytes(tr, blockHash));
        if (result == null ) return null;

        // We recover the "numTxs" property and feed it with that:
        long numTxs = toLong(read(tr, fullKeyForBlockNumTxs(tr, blockHash)));
        result = result.toBuilder().numTxs(numTxs).build();

        return result;
    }

    default long _getBlockNumTxs(T tr, String blockHash) {
        byte[] key = fullKeyForBlockNumTxs(tr, blockHash);
        return toLong(read(tr, key));
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

    default void _removeTxLinkedtoBlock(T tr, String txHash, String blockHash) {
        remove(tr, fullKeyForTxBlock(tr, txHash, blockHash));
        List<String> blockHashes =_getBlockHashesLinkedToTx(tr, txHash);
        if (blockHashes.isEmpty()) {
            remove(tr, fullKeyForTx(tr, txHash));
        }
    }

    default void _removeTxBasicInfo(T tr, String txHash) {
        remove(tr, fullKeyForTx(tr, txHash));
    }

    default void _removeTx(T tr, String txHash) {
        remove(tr, fullKeyForTx(tr, txHash));
        List<String> blockHashes =_getBlockHashesLinkedToTx(tr, txHash);
        for (String blockHash : blockHashes)
            remove(tr, fullKeyForTxBlock(tr, txHash, blockHash));
    }

    default void _removeTxs(T tr, List<String> txHashes) {
        txHashes.forEach(h -> _removeTx(tr, h));
    }

    default List<String> _getBlockHashesLinkedToTx(T tr, String txHash) {
        List<String> result = new ArrayList<>();
        byte[] preffix = keyStartingWith(fullKey(fullKeyForTxs(), KEY_PREFFIX_TX_BLOCK  + txHash + KEY_SEPARATOR));
        String preffixStr = new String(preffix);
        Iterator<String> it = getIterator(tr, preffix, null, null, e -> extractBlockHashFromKey(keyFromItem(e)).get());
        while (it.hasNext()) result.add(it.next());
        return result;
    }

    default void _addBlockNumTxs(T tr, String blockHash, long numTxsToAdd) {
        byte[] keyBlockNumTxs = fullKeyForBlockNumTxs(tr, blockHash);
        byte[] numTxsValue = read(tr, keyBlockNumTxs);
        long numTxsLong = (numTxsValue != null) ? (toLong(numTxsValue) + numTxsToAdd) : numTxsToAdd;
        save(tr, keyBlockNumTxs, bytes(numTxsLong));
    }

    default void _linkTxToBlock(T tr, String txHash, String blockHash, byte[] blockDirFullKey) {
        // We add a Key in this Block subfolder for this Tx:
        save(tr, fullKeyForBlockTx(tr, blockDirFullKey, txHash), bytes(1L)); // the value is NOT important here...

        // We add a Key in the "TXs" folder
        save(tr, fullKeyForTxBlock(tr, txHash, blockHash), bytes(1L));
    }

    default void _linkTxToBlock(T tr, String txHash, String blockHash) {
        byte[] blockDirFullKey = fullKeyForBlockDir(tr, blockHash);
        _linkTxToBlock(tr, txHash, blockHash, blockDirFullKey);
        // There is also a Property where we save the number of Txs belonging to this Block. We update the Value:
        _addBlockNumTxs(tr, blockHash, 1);
    }

    default void _unlinkTxFromBlock(T tr, String txHash, String blockHash, byte[] blockDirFullKey) {
        // We remove a Key from this Block subfolder:
        remove(tr, fullKeyForBlockTx(tr, blockDirFullKey, txHash));

        // We remove the Key from the "Txs" folder:
        remove(tr, fullKeyForTxBlock(tr, txHash, blockHash));
    }

    default void _unlinkTxFromBlock(T tr, String txHash, String blockHash) {
        byte[] blockDirFullKey = fullKeyForBlockDir(tr, blockHash);
        _unlinkTxFromBlock(tr, txHash, blockHash, blockDirFullKey);
        // There is also a Property where we save the number of Txs belonging to this Block. We update the Value:
        _addBlockNumTxs(tr, blockHash, -1);
    }

    default void _unlinkTx(T tr, String txHash) {
        List<String> blocksLinked = _getBlockHashesLinkedToTx(tr, txHash);
        blocksLinked.forEach(blockHash -> _unlinkTxFromBlock(tr, txHash, blockHash));
    }

    default void _unlinkBlock(String blockHash) {
        // We locate the Block Subfolder and the Key to start iterating over:
        byte[] keyStart = fullKey(fullKeyForBlockDir(blockHash), null);

        // For each Tx in this Block, we remove the "tx_block" property in the "TXs2 directory:
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
        // We locate the Block Subfolder and the Key to start iterating over:
        byte[] keyStart = fullKey(fullKeyForBlockDir(blockHash), null);

        // For each Tx in this Block, we update its "blocks" property, removing the reference to this Block.
        KeyValueIterator<byte[], T> iterator = getIterator(keyStart, null, null, e -> keyFromItem(e));
        loopOverKeysAndRun(iterator, (tr, key) -> {
            String txHashTemp = new String(key);
            String txHash = extractTxHashFromKey(key).get();
            _removeTxLinkedtoBlock(tr, txHash, blockHash);
            txHashConsumer.accept(txHash);
        }, null);

        // Now we remove the whole Block directory:
        removeBlockDir(blockHash);
    }

    /* DB High-Level Operations: */

    @Override
    default void saveBlock(BlockHeader blockHeader) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                _saveBlock(tr, blockHeader);
                _triggerBlocksStoredEvent(Arrays.asList(blockHeader));
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void saveBlocks(List<BlockHeader> blockHeaders) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
            */
            List<List<BlockHeader>> subLists = Lists.partition(blockHeaders, getConfig().getTransactionBatchSize());
            for (List<BlockHeader> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () -> _saveBlocks(tr, subList));
            }
            _triggerBlocksStoredEvent(blockHeaders);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default boolean containsBlock(Sha256Wrapper blockHash) {
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
    default Optional<BlockHeader> getBlock(Sha256Wrapper blockHash) {
        try {
            getLock().readLock().lock();
            AtomicReference<BlockHeader> result = new AtomicReference<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> result.set(_getBlock(tr, blockHash.toString())));
            return Optional.ofNullable(result.get());
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default void removeBlock(Sha256Wrapper blockHash) {
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
    default void removeBlocks(List<Sha256Wrapper> blockHashes) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */

            List<List<Sha256Wrapper>> subLists = Lists.partition(blockHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Wrapper> subList : subLists) {
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
            byte[] startingWith = keyStartingWith(fullKey(fullKeyForBlocks(), KEY_PREFFIX_BLOCK));
            return numKeys(startingWith);
        } finally {
            getLock().readLock().unlock();
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
    default boolean containsTx(Sha256Wrapper txHash) {
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
    default Optional<Tx> getTx(Sha256Wrapper txHash) {
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
    default void removeTx(Sha256Wrapper txHash) {
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
    default void removeTxs(List<Sha256Wrapper> txHashes) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            List<List<Sha256Wrapper>> subLists = Lists.partition(txHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Wrapper> subList : subLists) {
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
    default List<Sha256Wrapper> getTxsNeeded(Sha256Wrapper txHash) {
        try {
            getLock().readLock().lock();
            List<Sha256Wrapper> result = new ArrayList<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                Tx tx = _getTx(tr, txHash.toString());
                if (tx == null) return;
                Set<Sha256Wrapper> txsNeeded = tx.getInputs().stream()
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
            byte[] startingWith = keyStartingWith(fullKey(fullKeyForTxs(), KEY_PREFFIX_TX));
            return numKeys(startingWith);
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default void linkTxToBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> _linkTxToBlock(tr, txHash.toString(), blockHash.toString()));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void linkTxsToBlock(List<Sha256Wrapper> txsHashes, Sha256Wrapper blockHash) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            byte[] blockDirFullKey = fullKeyForBlockDir(blockHash.toString());
            List<List<Sha256Wrapper>> subLists = Lists.partition(txsHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Wrapper> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () ->
                        subList.forEach(h -> _linkTxToBlock(tr, h.toString(), blockHash.toString(), blockDirFullKey))
                );
            }
            T tr = createTransaction();
            executeInTransaction(tr, () -> _addBlockNumTxs(tr, blockHash.toString(), txsHashes.size()));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkTxFromBlock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> _unlinkTxFromBlock(tr, txHash.toString(), blockHash.toString()));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkTxsFromBlock(List<Sha256Wrapper> txsHashes, Sha256Wrapper blockHash) {
        try {
            getLock().writeLock().lock();
            /*
                Any operation performed on a List of Items will need to be split into smaller lists, just to make sure
                each Transaction is small (some KeyValue vendors have limitations)
             */
            List<List<Sha256Wrapper>> subLists = Lists.partition(txsHashes, getConfig().getTransactionBatchSize());
            for (List<Sha256Wrapper> subList : subLists) {
                T tr = createTransaction();
                executeInTransaction(tr, () ->
                        subList.forEach(h -> _unlinkTxFromBlock(tr, h.toString(), blockHash.toString()))
                );
            }
            T tr = createTransaction();
            executeInTransaction(tr, () -> _addBlockNumTxs(tr, blockHash.toString(), -txsHashes.size()));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkTx(Sha256Wrapper txHash) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> _unlinkTx(tr, txHash.toString()));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void unlinkBlock(Sha256Wrapper blockHash) {
        try {
            getLock().writeLock().lock();
            _unlinkBlock(blockHash.toString());
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default boolean isTxLinkToblock(Sha256Wrapper txHash, Sha256Wrapper blockHash) {
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
    default List<Sha256Wrapper> getBlockHashLinkedToTx(Sha256Wrapper txHash) {
        try {
            getLock().readLock().lock();
            List<Sha256Wrapper> result = new ArrayList<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                List<String> hashes = _getBlockHashesLinkedToTx(tr, txHash.toString());
                result.addAll(hashes.stream().map(Sha256Wrapper::wrap).collect(Collectors.toList()));

            });
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Iterable<Sha256Wrapper> getBlockTxs(Sha256Wrapper blockHash) {
        try {
            getLock().readLock().lock();
            if (!containsBlock(blockHash)) return null; // Check

            byte[] keyPreffix = keyStartingWith(fullKey(fullKeyForBlockDir(blockHash.toString()), KEY_PREFFIX_TX_LINK));
            Function<E, Sha256Wrapper> buildKeyFunction = e -> {
                byte[] key = keyFromItem(e);
                return Sha256Wrapper.wrap(extractTxHashFromKey(key).get());
            };

            Iterator<Sha256Wrapper> it = getIterator(keyPreffix, null, null, buildKeyFunction);
            Iterable<Sha256Wrapper> result = () -> it;
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default long getBlockNumTxs(Sha256Wrapper blockHash) {
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
    default void saveBlockTxs(Sha256Wrapper blockHash, List<Tx> txs) {
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
                    _saveTxs(tr, subList);
                    subList.forEach(h -> _linkTxToBlock(tr, h.getHash().toString(), blockHash.toString(), blockDirFullKey));
                    _addBlockNumTxs(tr, blockHash.toString(), subList.size());
                });
            } // for...
            _triggerTxsStoredEvent(txs);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default void removeBlockTxs(Sha256Wrapper blockHash) {
        try {
            getLock().writeLock().lock();
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
            _removeBlockTxs(blockHash.toString(), txHashConsumer);

            // In case there are still Tx that have not been published in an Event...
            if (batchTxsRemoved.size() > 0) _triggerTxsRemovedEvent(batchTxsRemoved);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default Optional<BlocksCompareResult> compareBlocks(Sha256Wrapper blockHashA, Sha256Wrapper blockHashB) {
        try {
            getLock().readLock().lock();
            Optional<BlockHeader> blockHeaderA = getBlock(blockHashA);
            Optional<BlockHeader> blockHeaderB = getBlock(blockHashB);

            if (blockHeaderA.isEmpty() || blockHeaderB.isEmpty()) return Optional.empty();

            BlocksCompareResult.BlocksCompareResultBuilder resultBuilder = BlocksCompareResult.builder()
                    .blockA(blockHeaderA.get())
                    .blockB(blockHeaderB.get());

            byte[] keyPreffixA = keyStartingWith(fullKey(fullKeyForBlockDir(blockHashA.toString()), KEY_PREFFIX_TX_LINK));
            byte[] keyPreffixB = keyStartingWith(fullKey(fullKeyForBlockDir(blockHashB.toString()), KEY_PREFFIX_TX_LINK));

            // We create an Iterable for the TXs in common:
            Function<E, Sha256Wrapper> buildItemBy = e -> Sha256Wrapper.wrap(extractTxHashFromKey(keyFromItem(e)).get());

            BiPredicate<T, byte[]> commonKeyValid = (tr, k) -> _isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashB.toString());
            Iterator<Sha256Wrapper> commonIterator = getIterator(keyPreffixA, null, commonKeyValid, buildItemBy);
            Iterable<Sha256Wrapper> commonIterable = () -> commonIterator;
            resultBuilder.txsInCommonIt(commonIterable);

            // We create an Iterable for the TXs that are ONLY in the block A:

            BiPredicate<T, byte[]> onlyAKeyValid = (tr, k) -> !_isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashB.toString());
            Iterator<Sha256Wrapper> onlyAIterator = getIterator(keyPreffixA, null, onlyAKeyValid, buildItemBy);
            Iterable<Sha256Wrapper> onlyAIterable = () -> onlyAIterator;
            resultBuilder.txsOnlyInA(onlyAIterable);

            // We create an Iterable for the TXs that are ONLY in the block B:
            BiPredicate<T, byte[]> onlyBKeyValid = (tr, k) -> !_isTxLinkToBlock(tr, extractTxHashFromKey(k).get(), blockHashA.toString());
            Iterator<Sha256Wrapper> onlyBIterator = getIterator(keyPreffixB, null, onlyBKeyValid, buildItemBy);
            Iterable<Sha256Wrapper> onlyBIterable = () -> onlyBIterator;
            resultBuilder.txsOnlyInB(onlyBIterable);

            return Optional.of(resultBuilder.build());
        } finally {
            getLock().readLock().unlock();
        }
    }

    /* Events triggering Operations */

    default void _triggerBlocksStoredEvent(List<BlockHeader> blockHeaders) {
        if (isTriggerBlockEvents()) {
            List<Sha256Wrapper> blockHashes = blockHeaders.stream().map(b -> b.getHash()).collect(Collectors.toList());
            getEventBus().publish(BlocksSavedEvent.builder().blockHashes(blockHashes).build());
        }
    }

    default void _triggerBlocksRemovedEvent(List<Sha256Wrapper> blockHashes) {
        if (isTriggerBlockEvents())
            getEventBus().publish(BlocksRemovedEvent.builder().blockHashes(blockHashes).build());
    }

    default void _triggerTxsStoredEvent(List<Tx> txs) {
        if (isTriggerTxEvents()) {
            List<Sha256Wrapper> txHashes = txs.stream().map(tx -> tx.getHash()).collect(Collectors.toList());
            getEventBus().publish(TxsSavedEvent.builder().txHashes(txHashes).build());
        }
    }

    default void _triggerTxsRemovedEvent(List<Sha256Wrapper> txHashes) {
        if (isTriggerTxEvents())
            getEventBus().publish(TxsRemovedEvent.builder().txHashes(txHashes).build());
    }


}
