package com.nchain.jcl.store.foundationDB.common;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.tuple.Tuple;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.serialization.BlockHeaderSerializer;
import com.nchain.jcl.base.serialization.TxSerializer;
import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.store.foundationDB.common.FDBIterator;
import com.nchain.jcl.store.foundationDB.common.FDBSafeIterator;
import com.nchain.jcl.store.foundationDB.common.HashesList;
import com.nchain.jcl.store.foundationDB.common.HashesListSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class provides methods to manage Keys and Values used when inserting or reading data from the DB.
 * The structure of the DB is determiend by this class and is as follows:
 *  - The main stucture is broken down into directories:
 *      \Blockchain
 *          \[Chain-Name]: example: "\BSV-Main", "\BSV-Stnnet", etc
 *              \blocks
 *                  [keys storing block info, more info below]
 *                  \[block_hash]
 *              \txs
 *                  [keys storing txs info, more info below]
 *
 *   - Inside the directories, information is stored using Key/Value pairs:
 *
 *      - "b:[block_hash]: Stores a Block Header serialized
 *         example: "b:0000023eer03400dfdsdf0230sdfsdf023" = [Block header serialzied]
 *      - "b_p:[block_hash]:[property]": Stores an individual property
 *         example: "b_p:0000023eer03400dfdsdf0230sdfsdf023:numTxs" = 1500
 *      - "tx:[tx_hash]": Stores a Whole Tx serialized
 *         example: "tx:aswe58738918347asbosd987230912312321312: = [Tx serialized]
 *      - "tx_p:[tx_hash]:[property]": Stores an individual property
 *         examples:
 *              "tx_p:aswe58738918347asbosd987230912312321312:blocks" = ["0000023eer03400dfdsdf0230sdfsdf023","000000aasds22388912ssda99898ds"]
 *              "tx_p:aswe58738918347asbosd987230912312321312:txsNeeded" = ["erqw547932441asd76478812fas675123","12312312adasd1312312adasd2433"]
 */
@Slf4j
public class KeyValueUtils {

    /** Definition of the Directory Layers structure: */
    public static final String DIR_BLOCKCHAIN            = "blockchain";
    public static final String DIR_BLOCKS                = "blocks";
    public static final String DIR_TXS                   = "txs";

    /** preffixes/suffixes used in Keys: */
    public static final String KEY_PREFFIX_BLOCK         = "b:";           // A Whole Block
    public static final String KEY_PREFFIX_BLOCK_PROP    = "b_p:";         // Property suffix
    public static final String KEY_SUFFIX_BLOCK_NUMTXS   = ":numTxs";      // Property suffix: the number of Txs in a Block
    public static final String KEY_PREFFIX_TX            = "tx:";          // A whole Tx
    public static final String KEY_PREFFIX_TX_PROP       = "tx_p:";        // Property suffix
    public static final String KEY_SUFFIX_TX_BLOCKS      = ":blocks";      // Property suffix: The list of blocks this Tx is linked to
    public static final String KEY_SUFFIX_TX_NEEDED      = ":txsNeeded";   // Property suffix: List of TXs THIS Tx depends on
    public static final String KEY_PREFFIX_TX_LINK       = "tx_l:";        // A Key that represents a reference to a Tx

    // Serializers of complex objects, using the Bitcoin Serialization:
    private static final BlockHeaderSerializer  BLOCKH_SER  = BlockHeaderSerializer.getInstance();
    private static final TxSerializer           TX_SER      = TxSerializer.getInstance();
    private static final HashesListSerializer   HASHES_SER  = HashesListSerializer.getInstance();

    // Functions to generate Keys in String format:

    public static String keyForBlock(String blockHash)       { return KEY_PREFFIX_BLOCK + blockHash; }
    public static String keyForBlockNumTxs(String blockHash) { return KEY_PREFFIX_BLOCK_PROP + blockHash + KEY_SUFFIX_BLOCK_NUMTXS; }
    public static String keyForTx(String txHash)             { return KEY_PREFFIX_TX + txHash;}
    public static String keyForTxBlocks(String txHash)       { return KEY_PREFFIX_TX_PROP + txHash + KEY_SUFFIX_TX_BLOCKS;}
    public static String keyForTxsNeeded(String txHash)      { return KEY_PREFFIX_TX_PROP + txHash + KEY_SUFFIX_TX_NEEDED;}
    public static String keyForBlockDir(String blockHash)    { return blockHash;}
    public static String keyForBlockTx(String txHash)        { return KEY_PREFFIX_TX_LINK + txHash;}


    /** Builds and returns a KEY, made of a Tuple containing the Directory and the Key given. */
    public static byte[] key(DirectorySubspace dir, byte[] key) { return Tuple.from(dir.getKey(), key).pack(); }

    /** Builds and returns a KEY, made of a Tuple containing the Directory and the Key given. */
    public static byte[] key(DirectorySubspace dir)             { return key(dir, new byte[0]);}

    /** Builds and returns a KEY, made of a Tuple containing the Directory and the Key given. */
    public static byte[] key(DirectorySubspace dir, String key) { return key(dir, bytes(key)); }

    /** It returns the Key WITHOUT the last Byte, so this key can  be used in a "startsWith" comparison. */
    public static byte[] keyComparisonPreffix(byte[] key) {
        byte[] resultBytes = new byte[key.length - 1];
        System.arraycopy(key, 0, resultBytes, 0, key.length - 1);
        return resultBytes;
    }

    /** It returns the Key PLUS and Extra Byte, so this key can  be used in a "endsWithWith" comparison. */
    public static byte[] keyComparisonSuffix(byte[] key) {
        byte[] resultBytes = new byte[key.length + 1];
        System.arraycopy(key, 0, resultBytes, 0, key.length);
        return resultBytes;
    }

    /**
     * It takes a Key, and returns the same Key in String format, after all the extra bytes added by the "Tuple" API
     * have been removed.
     */
    public static Optional<String> cleanKey(String key) {
        // We make some assumptions here:
        // - all the Keys in the DB have been inserted using a Tuple. The Tuple adds extra bytes at the beginning,
        //   end and in the middle of the items that make up the Tuple.
        // - the Directory Layer is being used every time a Key is inserted, so that means that ALL the Keys in the DB
        //   have been created using a Tuple with just 2 elements: the Directory Layer, and the Key itself.
        //
        // So here, in order to return a "clean" key, we remove the extra bytes added by the Tuple when the Key was
        // inserted, and considering a Tuple of just 2 elements, we need to remove:
        //  - 5 bytes at the beginning
        //  - 1 byte at the end
        if (key == null) return Optional.empty();
        byte[] original = key.getBytes();
        byte[] cleanArray = new byte[original.length - 6];
        System.arraycopy(original, 5, cleanArray,0, original.length - 6);
        String result = new String(cleanArray);
        return Optional.of(result);
    }

    /** Given a Key, it extract the Tx Hash from it as long as the key contains a Tx_hash, otherwise it returns null */
    public static Optional<String> extractTxHashFromKey(String key) {
        Optional<String> result = Optional.empty();
        if (key == null) return result;
        String keyCleaned = cleanKey(key).get();
        if (keyCleaned.indexOf(KEY_PREFFIX_TX) != -1)
            result = Optional.of(keyCleaned.substring(keyCleaned.indexOf(":") + 1));
        if (keyCleaned.indexOf(KEY_PREFFIX_TX_PROP) != -1)
            result = Optional.of(keyCleaned.substring(keyCleaned.indexOf(":") + 1, keyCleaned.lastIndexOf(":")));
        if (keyCleaned.indexOf(KEY_PREFFIX_TX_LINK) != -1)
            result = Optional.of(keyCleaned.substring(keyCleaned.indexOf(":") + 1));
        return result;
    }

    /** Given a Key, it extract the Tx Hash from it as long as the key contains a Tx_hash, otherwise it returns null */
    public static Optional<String> extractTxHashFromKey(byte[] key) {
        return extractTxHashFromKey(new String(key));
    }

    /** Given a Key, it extract the Tx Hash from it as long as the key contains a Tx_hash, otherwise it returns null */
    public static Optional<String> extractBlockHashFromKey(String key) {
        Optional<String> result = Optional.empty();
        if (key == null) return result;
        String keyCleaned = cleanKey(key).get();
        if (keyCleaned.indexOf(KEY_PREFFIX_BLOCK) != -1)
            result = Optional.of(keyCleaned.substring(keyCleaned.indexOf(":") + 1));
        if (keyCleaned.indexOf(KEY_PREFFIX_BLOCK_PROP) != -1)
            result = Optional.of(keyCleaned.substring(keyCleaned.indexOf(":") + 1, keyCleaned.lastIndexOf(":")));
        return result;
    }

    /** Given a Key, it extract the Tx Hash from it as long as the key contains a Tx_hash, otherwise it returns null */
    public static Optional<String> extractBlockHashFromKey(byte[] key) {
        return extractBlockHashFromKey(new String(key));
    }

    // Functions to serialize different types to byte arrays:

    public static byte[] bytes(BlockHeader header)          { return BLOCKH_SER.serialize(header); }
    public static byte[] bytes(Tx tx)                       { return TX_SER.serialize(tx);}
    public static byte[] bytes(Long value)                  { return ByteTools.uint64ToByteArrayLE(value); }
    public static byte[] bytes(HashesList hashes)           { return HASHES_SER.serialize(hashes);}
    public static byte[] bytes(String value)                { return value.getBytes();}

    // Functions to deserialize byte[] into other types/objects:

    public static boolean     isBytesOk(byte[] bytes)       { return (bytes != null && bytes.length > 0);}
    public static BlockHeader toBlockHeader(byte[] bytes)   { return (isBytesOk(bytes)) ? BLOCKH_SER.deserialize(bytes) : null;}
    public static Tx          toTx(byte[] bytes)            { return (isBytesOk(bytes)) ? TX_SER.deserialize(bytes) : null;}
    public static HashesList  toHashes(byte[] bytes)        { return (isBytesOk(bytes)) ? HASHES_SER.deserialize(bytes) : null;}
    public static long        toLong(byte[] bytes)          { return ByteTools.readInt64LE(bytes);}

    /**
     * Convenience method to print the content of a Directory, including sub-folders and (optionally) keys.
     */
    public static void printDir(Transaction tr, DirectorySubspace parentDir, int level, boolean showDirContent) {

        String tabulation = Strings.repeat("  ", level); // deeper elements go further to the right when printed
        log.info(tabulation + "\\" + Iterables.getLast(parentDir.getPath()).toUpperCase() + " " + _keyAsString(parentDir.getKey())) ;
        if (showDirContent) {
            // We iterate over the Keys stored within this Directory:
            Iterator<byte[]> keysIt = FDBIterator.<byte[]>iteratorBuilder()
                                            .transaction(tr)
                                            .fromDir(parentDir)
                                            .buildItemBy((kv) -> kv.getKey())
                                            .build();
            while (keysIt.hasNext()) {
                byte[] key = keysIt.next();
                log.info(tabulation + " - " + cleanKey(new String(key)).get() + " " + _keyAsString(key));
            }
        }

        // We do the same for its Subfolders
        level++;
        for (String dir : parentDir.list(tr).join()) {
            printDir(tr, parentDir.open(tr, Arrays.asList(dir)).join(), level, showDirContent);
        }
    }

    /**
     * Returns a String representation of a Key, for logging...
     */
    public static String _keyAsString(byte[] array) {
        StringBuffer result = new StringBuffer().append("[");
        for (int i = 0; i < array.length; i++) { result.append((i > 0)? "," : "").append(array[i]); }
        return result.append("]").toString();
    }

    /**
     * It iterates over a series of Keys in the DB, and performs operations over them.
     *
     * @param iterator              Iterator used to iterate over the Keys. This iterator is already configured to
     *                              start iterating at a specific position (keyPreffix).
     * @param startingKeyIndex      Index to move the iterator to before starting. If ZERO, the iterator starts at
     *                              the KeyPreffix its configured at, fi ONE it moves one position before starting, etc.
     * @param maxKeysToProcess      If specified, te iterator stops when this number of Keys have been reached.
     * @param taskForKey            If specified, this task is executed per each Key the iterator finds.
     * @param taskForAllKeys        If specified this task is executed ONCE, after iterating over all the Keys. It takes
     *                              all the kys as a parameters, so you must make sure this task is only specified when
     *                              the number of keys is gonna be manageable.
     */
    private static void loopOverKeysAndRun(FDBIterator<byte[]> iterator,
                                          Long startingKeyIndex,
                                          Optional<Long> maxKeysToProcess,
                                          BiConsumer<Transaction, byte[]> taskForKey,
                                          BiConsumer<Transaction, List<byte[]>> taskForAllKeys) {

        // We store each one of the Keys we process, so we can also trigger the global "taskForAllKeys", passing
        // all of them as a parameter. This list could be potentially huge, so we only use it if the
        // "taskForAllKeys" paraemeters has been set (not null)
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

    /**
     * It iterates over a series of Keys in the DB, performing an operation over them as it iterates.
     *
     * IMPORTANT:
     * This method uses an already existing Transaction (given as a parameter), so that means that the Items we are
     * going to iterate over are NOT too many, otherwise we might break the Transaction limitations.
     * more about FoundationDb limitations: https://apple.github.io/foundationdb/known-limitations.html
     *
     * @param tr                    Database Transaction
     * @param keyPrefix             Key Preffix to start iterating over
     * @param keySuffix             If specified, only those keys ending with these suffix will be processed.
     * @param startingKeyIndex      If specified, we start iterating on this position instead of the first one
     * @param maxKeysToProcess      If specified, the iteration stops after processing this many keys.
     * @param taskForKey            If specified, this task is executed for each Key
     * @param taskForAllKeys        If specified, this task is executed ONCE after the iteration is over, and it
     *                              takes ALL the kes as a parameter. ONLY use this is fyou are sure the number of
     *                              Keys is not going to be high, otherwise you might come across memory problems.
     */
    public static void loopOverKeysAndRun(Transaction tr,
                                          byte[] keyPrefix,
                                          byte[] keySuffix,
                                          Long startingKeyIndex,
                                          Optional<Long> maxKeysToProcess,
                                          BiConsumer<Transaction, byte[]> taskForKey,
                                          BiConsumer<Transaction, List<byte[]>> taskForAllKeys) {

        // We create an iterator and place it at the KeyPreffix:
        FDBIterator<byte[]> iterator = FDBIterator.<byte[]>iteratorBuilder()
                .transaction(tr)
                .startingWithPreffix(keyPrefix)
                .endingWithSuffix(keySuffix)
                .buildItemBy((kv) -> kv.getKey())
                .build();
        loopOverKeysAndRun(iterator, startingKeyIndex, maxKeysToProcess, taskForKey, taskForAllKeys);
    }

    /**
     * It iterates over a series of Keys in the DB, performing an operation over them as it iterates.
     *
     * IMPORTANT:
     * This method does NOT reuse an existing Transaction, instead it creates (and close) Transactions as it needs
     * while it iterates over the Keys. It foes it this wau in order to overcome the FoundationDB limitations:
     * https://apple.github.io/foundationdb/known-limitations.html
     *
     * @param db                    Database
     * @param keyPrefix             Key Preffix to start iterating over
     * @param keySuffix             If specified, only those keys ending with these suffix will be processed.
     * @param startingKeyIndex      If specified, we start iterating on this position instead of the first one
     * @param maxKeysToProcess      If specified, the iteration stops after processing this many keys.
     * @param taskForKey            If specified, this task is executed for each Key
     * @param taskForAllKeys        If specified, this task is executed ONCE after the iteration is over, and it
     *                              takes ALL the kes as a parameter. ONLY use this is fyou are sure the number of
     *                              Keys is not going to be high, otherwise you might come across memory problems.
     */
    public static void loopOverKeysAndRun(Database db,
                                          byte[] keyPrefix,
                                          byte[] keySuffix,
                                          Long startingKeyIndex,
                                          Optional<Long> maxKeysToProcess,
                                          BiConsumer<Transaction, byte[]> taskForKey,
                                          BiConsumer<Transaction, List<byte[]>> taskForAllKeys) {
        try {
            // We create a "Safe"  iterator and place it at the KeyPreffix:
            FDBSafeIterator<byte[]> iterator = FDBSafeIterator.<byte[]>longIteratorBuilder()
                    .database(db)
                    .startingWithPreffix(keyPrefix)
                    .endingWithSuffix(keySuffix)
                    .buildItemBy((kv) -> kv.getKey())
                    .build();
            loopOverKeysAndRun(iterator, startingKeyIndex, maxKeysToProcess, taskForKey, taskForAllKeys);

            // We close the lat Transaction crated by the iterator after finishing:
            iterator.getCurrentTransaction().commit().get();
            iterator.getCurrentTransaction().close();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /** Convenience method with no limit, no suffix and no "taskforAllKeys" */
    public static void loopOverKeysAndRun(Transaction tr, byte[] keyPrefix, BiConsumer<Transaction, byte[]> taskForKey) {
        loopOverKeysAndRun(tr,keyPrefix, null, 0L, Optional.empty(), taskForKey, null);
    }

    public static void loopOverKeysAndRun(Database db, byte[] keyPrefix, BiConsumer<Transaction, byte[]> taskForKey) {
        loopOverKeysAndRun(db,keyPrefix, null, 0L, Optional.empty(), taskForKey, null);
    }


    /**
     * Returns the Number of Keys that belong to the directory specified and which Key starts with the preffix given.
     */
    public static long numKeys(Database db, DirectorySubspace dir, byte[] keyPreffix) {
        AtomicLong result = new AtomicLong();
        byte[] keyPreffixToCompare = keyComparisonPreffix(key(dir, keyPreffix));
        loopOverKeysAndRun(db, keyPreffixToCompare, (tr, k) -> result.incrementAndGet());
        return result.get();
    }

    /** Indicates if a Key starts with the preffix given */
    public static boolean keyStartsWith(byte[] key, byte[] preffix) {
        for (int i = 0; i < preffix.length; i++)
            if (preffix[i] != key[i]) return false;
        return true;
    }

    /** Indicates if a Key ends with the suffix given */
    public static boolean keyEndsWith(byte[] key, byte[] suffix) {
        for (int i = 1; i <= suffix.length; i++)
            if (suffix[suffix.length -i ] != key[key.length -i]) return false;
        return true;
    }

}
