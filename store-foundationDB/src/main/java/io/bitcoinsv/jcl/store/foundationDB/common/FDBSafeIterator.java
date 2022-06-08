package io.bitcoinsv.jcl.store.foundationDB.common;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.directory.DirectorySubspace;
import io.bitcoinsv.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of an Iterator that iterates over the Keys in a FoundationDB database.
 *
 * IMPORTANT:
 * An iteration over Keys in a FoundationDb needs a Transaction in order to work. But due to the FoundationDB
 * limitations, this process might break if the number of Keys is too high. For that reason, this Iterator takes
 * care of that situation by creating, closing and creating new Transactions on-the-fly as it iterates over the Keys,
 * BEFORE breaking those limits.
 *
 * NOTE: A consecuence of this is obvious: If the numebr of elements is so big that the Transaction is resetted,
 * committed, and a new one is created, then that means that we do NOT have Atomic Transactions anymore, since
 * several and diffrente Transactiosn might have been created while the iterator has been running. This is a natural
 * consecuence, but there is no other way to work around the limitation of FoundatinDB.
 *
 * more info about FoundationDB limitations: https://apple.github.io/foundationdb/known-limitations.html
 */
public class FDBSafeIterator<T> extends FDBIterator<T> implements Iterator<T> {

    // Connection to the DB:
    private Database db;

    // Keeps track of the Maximum number of Items to process before closing the current Transaction
    // and creating a new one:
    private long maxItemsToProcess = BlockStoreFDBConfig.TRANSACTION_BATCH_SIZE; // Default
    private long numItemsProcessed = 0;

    // It resets the iterator, closing the current Transaction and creating a new One:
    private void resetIterator(byte[] keyStart) {
        currentTransaction.next(false);

        // We reset the iterator, pointing it at the next Key to process...
        fdbIterator = currentTransaction.getRange(keyStart, "\\xff".getBytes()).iterator();
        fdbIterator.next(); // we skip the first one (already processed)
    }

    public FDBSafeIterator(Database database,
                           DirectorySubspace incompleteTxsDir,
                           byte[] startingWithPreffix,
                           byte[] endingWithSuffix,
                           BiPredicate<LargeTransaction, byte[]> keyIsValidWhen,
                           Function<KeyValue, T> buildItemBy,
                           Long maxItemsToProcess) {
        super(new LargeTransaction(database, incompleteTxsDir, BlockStoreFDBConfig.TRANSACTION_MAX_SIZE_BYTES), startingWithPreffix, endingWithSuffix, keyIsValidWhen, buildItemBy);
        this.db = database;
        if (maxItemsToProcess != null) this.maxItemsToProcess = maxItemsToProcess;
    }

    // Returns the next Key in the DB, or null if there is no one. If also detects if the Transaction needs to be reset
    @Override
    protected byte[] nextKey() {
        byte[] result = super.nextKey();
        if (this.lastEntryProcessed != null) {
            numItemsProcessed++;
            if (numItemsProcessed == maxItemsToProcess) {
                resetIterator(lastEntryProcessed.getKey());
                numItemsProcessed = 1;
            }
        }
        return result;
    }

    public static <T> FDBSafeIteratorBuilder<T> safeBuilder() {
        return new FDBSafeIteratorBuilder<T>();
    }

    /**
     * Builder
     * @param <T> Class of each Item returned by the Iterator
     */
    public static class FDBSafeIteratorBuilder<T> {
        private Database database;
        private DirectorySubspace incompleteTxsDir;
        private byte[] startingWithPreffix;
        private byte[] endingWithSuffix;
        private BiPredicate<LargeTransaction, byte[]> keyIsValidWhen;
        private Function<KeyValue, T> buildItemBy;
        private Long maxItemsToProcess;

        FDBSafeIteratorBuilder() {
        }

        public FDBSafeIteratorBuilder<T> database(Database database) {
            this.database = database;
            return this;
        }

        public FDBSafeIteratorBuilder<T> incompleteTxsDir(DirectorySubspace incompleteTxsDir) {
            this.incompleteTxsDir = incompleteTxsDir;
            return this;
        }

        public FDBSafeIteratorBuilder<T> startingWithPreffix(byte[] startingWithPreffix) {
            this.startingWithPreffix = startingWithPreffix;
            return this;
        }

        public FDBSafeIteratorBuilder<T> endingWithSuffix(byte[] endingWithSuffix) {
            this.endingWithSuffix = endingWithSuffix;
            return this;
        }

        public FDBSafeIteratorBuilder<T> keyIsValidWhen(BiPredicate<LargeTransaction, byte[]> keyIsValidWhen) {
            this.keyIsValidWhen = keyIsValidWhen;
            return this;
        }

        public FDBSafeIteratorBuilder<T> buildItemBy(Function<KeyValue, T> buildItemBy) {
            this.buildItemBy = buildItemBy;
            return this;
        }

        public FDBSafeIteratorBuilder<T> maxItemsToProcess(Long maxItemsToProcess) {
            this.maxItemsToProcess = maxItemsToProcess;
            return this;
        }

        public FDBSafeIterator<T> build() {
            return new FDBSafeIterator<T>(database, incompleteTxsDir, startingWithPreffix, endingWithSuffix, keyIsValidWhen, buildItemBy, maxItemsToProcess);
        }

        public String toString() {
            return "FDBSafeIterator.FDBSafeIteratorBuilder(database=" + this.database + "incompleteTxsDir="+ incompleteTxsDir + ", startingWithPreffix=" + java.util.Arrays.toString(this.startingWithPreffix) + ", endingWithSuffix=" + java.util.Arrays.toString(this.endingWithSuffix) + ", keyIsValidWhen=" + this.keyIsValidWhen + ", buildItemBy=" + this.buildItemBy + ", maxItemsToProcess=" + this.maxItemsToProcess + ")";
        }
    }
}
