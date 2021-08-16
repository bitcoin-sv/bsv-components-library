/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.foundationDB.common;


import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import io.bitcoinsv.jcl.store.keyValue.common.KeyValueIteratorImpl;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class is an implementation of an Iterator that fetches the data from a FundationDB DataBase.
 * It extends the functionality of KeyValueIteratorIml, and provides specific implementations for those methods that
 * need to be rewritten, since they depen on specific implemenation-details.
 *
 * FoundationDB Supports Transactions, and an Iterator needs to be linked to one. In the most common scenario, the
 * iterator will run in a piece of code where a DB Transaction is already open, so the Iterator will be created and
 * linked to that Transaction. So this implementation NEEDS an already existing Transaction, which si fed into the
 * Constructor. FoundationDB has some limits when it comes to Transactions:
 *  - Transactions can NOt last longer than 5 seconds
 *  - Data modified in a Transaction cannot be large than a specific amount fo bytes.
 *
 *  So that measn that if this Iterator is open and condifured to iteratoe over a set of Items and it breaks any of
 *  the conditions above, it will throw an Exception.
 *  So, as a regular basis, this Iterartor will ONLY be used when the data to loop over is not extremely big, and the
 *  whole loop does not take longer than 5 seconds.
 *
 *  If you are not sure about the time it will take, or you already know that the Iterator might have to loop over a
 *  huge number of items, use insted the "FDBSafeIterator".
 *
 * @see FDBSafeIterator
 *
 * more info about FoundationDB limitations: https://apple.github.io/foundationdb/known-limitations.html
 *
 */
public class FDBIterator<I> extends KeyValueIteratorImpl<I, Transaction, KeyValue> implements Iterator<I> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FDBIterator.class);
    // Internal iterator on a FoundationDb connection
    protected Iterator<KeyValue> fdbIterator;

    // Current FoundationDB Transaction
    protected Transaction currentTransaction;

    /**
     * Constructor
     */
    public FDBIterator(Transaction currentTransaction,
                       byte[] startingWithPreffix,
                       byte[] endingWithSuffix,
                       BiPredicate<Transaction, byte[]> keyIsValidWhen,
                       Function<KeyValue, I> buildItemBy) {

        super(startingWithPreffix, endingWithSuffix, keyIsValidWhen, buildItemBy);

        // Argument check:
        checkArgument(currentTransaction != null, "a Transaction must be specified");
        checkArgument(startingWithPreffix != null, "a Preffix must be specified");
        checkArgument(buildItemBy != null, "A 'buildItemBy' function must be specified");

        // We init the basic properties:
        this.currentTransaction = currentTransaction;

        // We init the FDB Iterator:
        this.fdbIterator = currentTransaction.getRange(keyPreffix, "\\xff".getBytes()).iterator();
    }

    @Override protected boolean  hasNextItemFromDB()            { return fdbIterator.hasNext(); }
    @Override protected KeyValue nextEntryFromDB()              { return fdbIterator.next(); }
    @Override protected byte[] getKeyFromEntry(KeyValue item)   { return item.getKey(); }
    @Override public Transaction getCurrentTransaction()        { return currentTransaction; }

    public static <I> FDBIteratorBuilder<I> builder() {
        return new FDBIteratorBuilder<I>();
    }

    /**
     * Builder
     * @param <I> Class of each Item returned by the Iterator
     */
    public static class FDBIteratorBuilder<I> {
        private Transaction currentTransaction;
        private byte[] startingWithPreffix;
        private byte[] endingWithSuffix;
        private BiPredicate<Transaction, byte[]> keyIsValidWhen;
        private Function<KeyValue, I> buildItemBy;

        FDBIteratorBuilder() {
        }

        public FDBIterator.FDBIteratorBuilder<I> currentTransaction(Transaction currentTransaction) {
            this.currentTransaction = currentTransaction;
            return this;
        }

        public FDBIterator.FDBIteratorBuilder<I> startingWithPreffix(byte[] startingWithPreffix) {
            this.startingWithPreffix = startingWithPreffix;
            return this;
        }

        public FDBIterator.FDBIteratorBuilder<I> endingWithSuffix(byte[] endingWithSuffix) {
            this.endingWithSuffix = endingWithSuffix;
            return this;
        }

        public FDBIterator.FDBIteratorBuilder<I> keyIsValidWhen(BiPredicate<Transaction, byte[]> keyIsValidWhen) {
            this.keyIsValidWhen = keyIsValidWhen;
            return this;
        }

        public FDBIterator.FDBIteratorBuilder<I> buildItemBy(Function<KeyValue, I> buildItemBy) {
            this.buildItemBy = buildItemBy;
            return this;
        }

        public FDBIterator<I> build() {
            return new FDBIterator<I>(currentTransaction, startingWithPreffix, endingWithSuffix, keyIsValidWhen, buildItemBy);
        }

    }
}
