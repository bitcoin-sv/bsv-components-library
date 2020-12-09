package com.nchain.jcl.store.foundationDB.common;

import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectorySubspace;
import lombok.Builder;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.nchain.jcl.store.foundationDB.common.KeyValueUtils.*;
import static com.google.common.base.Preconditions.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of an Iterator that iterates over the Keys in a FoundationDB database.*
 * The iterator can be configured in different ways, to secifiy which keys we want to iterate over:
 * - We can specify a Directory, so ONLY those Keys belonging to that Directory will be processed
 * - We can specify a Suffix, so ONLY those Keys ENDING with that suffix will be processed
 * - We can specifiy a PREFFIX, so ONLY those keys STARTING wiht that preffix will be processed.
 *
 * IMPORTANT:
 * This iterator only works if its executed within an Open Transaction, otherwise, it will throw an
 * IllegalState Exception. If the number of items is too high, this iterator might break due to the Foundation
 * limitations, so make sure you only use it when you can be fairly sure that the number of Keys is limited.
 *
 * For a "safer" Iterator that can work with any number of Items, see {@link FDBSafeIterator}
 *
 */
public class FDBIterator<T> implements Iterator<T> {

    // Connection to the DB:
    protected Transaction tr;

    // Keys Range definition:
    protected byte[] keyPreffix;
    protected byte[] keyStart;
    protected byte[] keySuffix;
    protected DirectorySubspace dir;
    protected BiPredicate<Transaction, byte[]> verificationKey;

    protected Iterator<KeyValue> iterator;

    // Last item returned by "next()"
    protected KeyValue lastItemProcessed = null;

    // Function executed to return each Item in the "nextItem()" calls:
    protected Function<KeyValue, T> itemFunction;


    @Builder(builderMethodName = "iteratorBuilder")
    public FDBIterator(Transaction transaction, DirectorySubspace fromDir,
                       byte[] startingWithPreffix,
                       byte[] startingAtKey,
                       byte[] endingWithSuffix,
                       BiPredicate<Transaction, byte[]> keyIsValidWhen,
                       Function<KeyValue, T> buildItemBy) {
        // Argument check:
        checkArgument(transaction != null, "a Transaction must be specified");
        checkArgument(fromDir != null || startingWithPreffix != null || startingAtKey != null, "" +
                "at least a Directory or a Preffix ot a Starting Key must be specified");
        checkArgument(buildItemBy != null, "A 'buildItemBy' function must be specified");


        // Initialization:

        this.tr = transaction;
        this.dir = fromDir;
        this.keyPreffix = (fromDir == null)
                ? startingWithPreffix
                : (startingWithPreffix == null)
                    ? keyComparisonPreffix(key(fromDir, new byte[0]))
                    : keyComparisonPreffix(key(fromDir, startingWithPreffix));
        this.keyStart = startingAtKey;

        this.keySuffix = (fromDir == null)
                ? endingWithSuffix
                : (endingWithSuffix == null)
                    ? null
                    : keyComparisonSuffix(endingWithSuffix);

        if (keyStart != null)   this.iterator = tr.getRange(keyStart, "\\xff".getBytes()).iterator();
        else                    this.iterator = tr.getRange(keyPreffix, "\\xff".getBytes()).iterator();

        this.verificationKey = keyIsValidWhen;
        this.itemFunction = buildItemBy;
    }

    // Returns the next Key in the DB, or null if there is no one.
    protected byte[] nextKey() {
        this.lastItemProcessed =  (iterator.hasNext()) ? iterator.next() : null;
        return (lastItemProcessed != null)? lastItemProcessed.getKey() : null;
    }

    // Indicates if the Key is Valid considering this Iterator Configuration
    private boolean isKeyValid(byte[] key) {
        boolean result = (key != null);
        if (result) result &= (keyPreffix == null || KeyValueUtils.keyStartsWith(key, keyPreffix));
        if (result) result &= (keySuffix == null || KeyValueUtils.keyEndsWith(key, keySuffix));
        if (result) result &= (verificationKey == null || verificationKey.test(tr ,key));
        return result;
    }

    // Indicates if the KEY belongs to the Directory given
    private boolean isKeyBelongsToDir(DirectorySubspace dir, byte[] key) {
        byte[] dirKey = keyComparisonPreffix(key(dir));
        boolean result = keyStartsWith(key, dirKey);
        return result;
    }

    // It pulls the iterator until it reaches the end, or we get the next VALID Key.
    private byte[] nextValidKey() {
        byte[] nextKey;
        String nextKeyStr;

        do {
            nextKey = nextKey();
            //nextKeyStr = new String((nextKey != null) ? nextKey : new byte[0]);
            //System.out.println(" ---> nextKey pulled: " + nextKeyStr);
            // If we reach the end of the iterator, we stop looking...
            if (nextKey == null) break;
            // If the Key is Valid, we Stop looking...
            if (isKeyValid(nextKey)) break;
            // If a Dir has been specified and the Key does NOT belong to it, we Stop looking...
            if (dir != null && !isKeyBelongsToDir(dir, nextKey)) break;
            // If a Verification Function has been specified
            if (verificationKey == null && keySuffix == null) break;
        } while (true);

        return isKeyValid(nextKey)? nextKey : null;
    }

    @Override
    public boolean hasNext() {

        /*
           It's not enough to indicate if there are more Keys in the DB. We need to indicate if there are more
           VALID Keys. And its not enough either to chekc if the NEXT Keu is valid or not, because some times the
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
    public T next() {
        if (lastItemProcessed == null) nextValidKey();
        T result = itemFunction.apply(lastItemProcessed);
        lastItemProcessed = null;
        return result;
    }

    public Transaction getCurrentTransaction() {
        return this.tr;
    }

}
