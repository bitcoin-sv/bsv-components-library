package com.nchain.jcl.store.keyValue.common;

import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Base implementation of the KeyValueIterator. Even though each DB-specific Iterator will have to implement their
 * own DB Connection logic, some functionalities are common to all of them. This class provides the common
 * functionality, which is getting the next element form the DB, checking if its correct, and also checking if there
 * are more elements in the Db. These operations, in turn, will rely on inner methods that will need to be developed
 * by an implementation.specific class.
 *
 * Its possible to parametrize:
 *
 *  - The preffix: The iterator will start from the first Key that starts withthe given preffix.
 *
 *  - The suffix: The Iterator will only return thos Kes that ends witht he given suffix.
 *
 *  - The Item-BuilderFunction: For each Key, the Iterator will validate it agains the parameters (the preffix and
 *    suffix) above, and if valid, it will return a new "Item", which can be retrieved from the Client-side by calling
 *    the "next()" method. This "item" might be the Key itself, or the Value, or any other value. In order to make this
 *    generic, a Function can be specified, which will be invoked to "build" each Item returned by the "next()" method.
 *
 *  - The "keyVerifier" function: The Iterator will only process those KEys that are Valid, and a Key is valid when
 *    it starts with the given Preffix and ends with the given Suffix. But an additional validation can be also
 *    specified by the "keyVerifier" function, which is a Predicate that is applied over each Key. Thi sis useful if we
 *    only want to return those Keys that fullfill some conditions.
 *
 *
 * @param <I>   Type of the ITEM returned by the Iterator ("next()" method)
 * @param <T>   Type of the TRANSACTION used by the DB-specific implementation. If Transactions are NOT suported, the
 *              "Object" Type can be used.
 * @param <E>   Type of each ENTRY in the DB. Each Key-Value DB implementation usually provides Iterators that returns
 *              Entries from the DB (KeyValue in FoundationDB, a Map.Entry in LevelDb, etc).
 *
 *
 *  Any class extending this class will usually need to have a reference to the "DB-specific" iterator, which is the
 *  one that iterates over the "real" data from the DB. that data will get received in this class, the verifications
 *           will be applied on it, and the final "Items" wilbe returned to the Client.
 */
public abstract class KeyValueIteratorImpl<I,T,E> implements KeyValueIterator<I, T> {

    // Keys Range definition:
    protected byte[] keyPreffix;
    protected byte[] keySuffix;

    // Function to verify if each Key is Valid (apart form the Preffix & suffix Verification)
    protected BiPredicate<T, byte[]> keyVerifier;

    // Function to build each Item returned by the "nert()" method
    protected Function<E, I> itemFunction;

    // Last item returned by "next()"
    protected E lastEntryProcessed = null;

    /**
     * Indicates if the DB contains any more Keys. This is usually a call to the "hasNext()" method to the
     * internal/DB-specific iterator.
     */
    protected abstract boolean hasNextItemFromDB();

    /**
     * Returns the Next Entry from the DB. This is usually a call to the "next()" method to the internal/DB-specific
     * iterator
     */
    protected abstract E nextEntryFromDB();

    /**
     * Returns the KEY form the Entry given.
     * @param  entry Item returned by the internal/Db-specific iterator "next()" method.
     * @return the Key in byte[] format.
     */
    protected abstract byte[] getKeyFromEntry(E entry);

    /**
     * Constructor
     *
     * @param startingWith      Preffix: The Keys processed will need to start with it.
     * @param endingWith        Suffix: The keys processed will beed to end with it.
     * @param keyIsValidWhen    Verification Function that is applied over each Key, only valid keys will be processed
     * @param buildItemBy       Function that is invoked to build each Item that will be returned by the "next()" method.
     */
    public KeyValueIteratorImpl(byte[] startingWith,
                                byte[] endingWith,
                                BiPredicate<T, byte[]> keyIsValidWhen,
                                Function<E, I> buildItemBy) {
        this.keyPreffix = (startingWith != null)? startingWith : new byte[0];
        this.keySuffix = endingWith;
        this.keyVerifier = keyIsValidWhen;
        this.itemFunction = buildItemBy;
    }

    /** Indicates if a Key starts with the preffix given */
    private boolean keyStartsWith(byte[] key, byte[] preffix) {
        for (int i = 0; i < preffix.length; i++)
            if (preffix[i] != key[i]) return false;
        return true;
    }

    /** Indicates if a Key ends with the suffix given */
    private boolean keyEndsWith(byte[] key, byte[] suffix) {
        for (int i = 1; i <= suffix.length; i++)
            if (suffix[suffix.length -i ] != key[key.length -i]) return false;
        return true;
    }

    // Returns the next Key in the DB, or null if there is no one.
    protected byte[] nextKey() {
        this.lastEntryProcessed =  (hasNextItemFromDB()) ? nextEntryFromDB() : null;
        return (lastEntryProcessed != null)? getKeyFromEntry(lastEntryProcessed) : null;
    }

    // Indicates if the Key is Valid considering the Preffix configured
    private boolean isKeyPreffixValid(byte[] key) {
        return (key != null) && (keyPreffix == null || keyStartsWith(key, keyPreffix));
    }

    // Indicates if the Key is Valid considering the Suffix configured
    private boolean isKeySuffixValid(byte[] key) {
        return (key != null) && (keySuffix == null || keyEndsWith(key, keySuffix));
    }

    // Indicates if the Key is Valid considering the Key Verifier configured
    private boolean isKeyVerifierValid(byte[] key) {
        return (key != null) && (keyVerifier == null || keyVerifier.test(getCurrentTransaction() ,key));
    }

    // Indicates if the Key is Valid considering this Iterator Configuration
    private boolean isKeyValid(byte[] key) {
        boolean result = isKeyPreffixValid(key) && isKeySuffixValid(key) && isKeyVerifierValid(key);
        return result;
    }

    // It pulls the iterator until it reaches the end, or we get the next VALID Key.
    private byte[] nextValidKey() {
        byte[] nextKey;
        do {
            nextKey = nextKey();

            // If we reach the end of the iterator, we stop looking...
            if (nextKey == null) break;
            // If the Key does not meet the Preffix, we stop looking...
            if (!isKeyPreffixValid(nextKey)) break;

            // If the fullKey is Valid, we stop looking:
            if (isKeyValid(nextKey)) break;

            // IF we reach this far, then the Key is INVALID. If no Suffix or KeyVErifier has been secified we just stop
            if (keyVerifier == null && keySuffix == null) break;
        } while (true);
        return isKeyValid(nextKey)? nextKey : null;
    }

    @Override
    public boolean hasNext() {

        /*
           It's not enough to indicate if there are more Keys in the DB. We need to indicate if there are more
           VALID Keys. And its not enough either to check if the NEXT Key is valid or not, because some times the
           valid Keys are not in sequence. For example, in this sequence of Keys:

            [1] "tx_p:122asd3221523sddsdaawe5112322220:blocks"
            [2] "tx_p:122asd3221523sddsdaawe5112322220:txsNeeded"
            [3] "tx_p:4332ssd22359231239asda9889092323:blocks"
            [4] "tx_p:4332ssd22359231239asda9889092323:txsNeeded"
            [5] "tx_p:998aasd2213886523321233ddssd0905:blocks"
            [6] "tx_p:998aasd2213886523321233ddssd0905:txsNeeded"

            - If the iterator is configured to look for the Keys starting with "tx_p:122asd3221523sddsdaawe5112322220",
            then only the Keys [1] and [2] wil be processed.
            - If the iterator is configured to look for the Keys ending with ":blocks" (and no preffix specified), then
                the Keys [1], [3] and [5] will be processed. And these Keys are not in sequence, they are "invalid"
                keys in between.
         */


        return (nextValidKey() != null);
    }

    @Override
    public I next() {
        if (lastEntryProcessed == null) nextValidKey();
        I result = itemFunction.apply(lastEntryProcessed);
        lastEntryProcessed = null;
        return result;
    }
}
