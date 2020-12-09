package com.nchain.jcl.store.foundationDB.common;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig;
import lombok.Builder;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.nchain.jcl.store.foundationDB.common.KeyValueUtils.key;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of an Iterator that iterates over the Keys in a FoundationDB database.
 *
 * IMPORTANT:
 * An iteration over Keys in a FoundationDb needs a Transaction in order to work. But due to the FoundationDB
 * imitations, this process might break if the number of Keys is too high. for that reason, this Iterator takes
 * care of that situation by creating, closing and creating new Transaction on-the-fluy as it iterates over the Keys,
 * making sure that it never reaches that limimtation.
 */
public class FDBSafeIterator<T> extends FDBIterator<T> implements Iterator<T> {

    // Connection to the DB:
    private Database db;

    // Keeps track of the Maximum number of Itesm to process before closin the current Transaction
    // and creating a new one:
    @Builder.Default
    private long maxItemsToProcess = BlockStoreFDBConfig.TRANSACTION_BATCH_SIZE;
    private long numItemsProcessed = 0;


    // It resets the iterator, closing the current Transaction and creating a new One:
    private void resetIterator(byte[] keyStart) {
        tr.close();
        tr = db.createTransaction();
        this.iterator = tr.getRange(keyStart, "\\xff".getBytes()).iterator();
        this.iterator.next(); // we skip the first one...
    }

    @Builder(builderMethodName = "longIteratorBuilder")
    public FDBSafeIterator(Database database, DirectorySubspace fromDir,
                           byte[] startingWithPreffix,
                           byte[] startingAtKey,
                           byte[] endingWithSuffix,
                           BiPredicate<Transaction, byte[]> keyIsValidWhen,
                           Function<KeyValue, T> buildItemBy,
                           Long maxItemsToProcess) {
        super(database.createTransaction(), fromDir, startingWithPreffix, startingAtKey, endingWithSuffix, keyIsValidWhen, buildItemBy);
        this.db = database;
        if (maxItemsToProcess != null) this.maxItemsToProcess = maxItemsToProcess;
    }

    // Returns the next Key in the DB, or null if there is no one.
    @Override
    protected byte[] nextKey() {
        byte[] result = super.nextKey();
        if (this.lastItemProcessed != null) {
            numItemsProcessed++;
            if (numItemsProcessed == maxItemsToProcess) {
                resetIterator(lastItemProcessed.getKey());
                numItemsProcessed = 1;
            }
        }
        return result;
    }
}
