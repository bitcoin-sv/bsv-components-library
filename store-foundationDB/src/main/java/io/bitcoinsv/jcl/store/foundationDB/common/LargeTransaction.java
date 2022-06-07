package io.bitcoinsv.jcl.store.foundationDB.common;

import com.apple.foundationdb.*;
import com.apple.foundationdb.async.AsyncIterable;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.tuple.Tuple;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 *
 * @author m.fletcher@nchain.com
 * @date 12/01/2022
 */
public class LargeTransaction {


    private int currentTransactionValueSizeBytes;
    private Transaction currentTransaction;
    private Database db;
    private int maxTransactionValueSizeBytes;
    private DirectorySubspace incompleteTxDir;
    private List<byte[]> unsavedKeys = new ArrayList<>();


    /*
     * This should only be used for reading, writing will require a DirectorySubspace
     */
    public LargeTransaction(Database db, DirectorySubspace incompleteTxDir, int maxTransactionValueSizeBytes) {
        this.db = db;
        currentTransaction = db.createTransaction();
        this.maxTransactionValueSizeBytes = maxTransactionValueSizeBytes;
        this.incompleteTxDir = incompleteTxDir;
    }

    public LargeTransaction(Database db, DirectorySubspace incompleteTxDir, Transaction transaction, int maxTransactionValueSizeBytes) {
        this.db = db;
        currentTransaction = transaction;
        this.maxTransactionValueSizeBytes = maxTransactionValueSizeBytes;
        this.incompleteTxDir = incompleteTxDir;
    }

    public synchronized Transaction getCurrentTransaction(){
        return currentTransaction;
    }

    /**
     * Commits {@literal &} closes the existing transaction and creates a new one, while saving references to its keyset. The keyset will be removed once the large transaction has been completely
     * commited.
     * @return
     */
    public synchronized Transaction next(boolean saveReferenceKeys){
        //Save these keys until we completely close this large tx then remove them. This way we can detect an unclean shutdown on startup and recover.
        if(saveReferenceKeys) {
            for(byte[] key: unsavedKeys) {
                currentTransaction.set(incompleteTxDir.subspace(Tuple.from(key)).getKey(), key);
            }
        }

        currentTransaction.commit().join();
        currentTransaction.close();

        currentTransaction = db.createTransaction();

        unsavedKeys.clear();
        currentTransactionValueSizeBytes = 0;

        return currentTransaction;
    }

    /**
     * Removes any entries saved that are references to keys that are only partly saved within this transaction. Once all reference keys are removed,
     * that is an indicator that the large transaction has been saved successfully.
     */
    public void cleanReferenceKeys(){
        currentTransaction.clear(incompleteTxDir.range());
    }

    /**
     * Commits the final transaction and cleans up any additional data that was saved to help make this class atomic.
     * @return
     */
    public synchronized CompletableFuture<Void> commit(){
        return CompletableFuture.supplyAsync(() -> {
            if(unsavedKeys.size() > 0) {
                cleanReferenceKeys();
            }
            return currentTransaction.commit().join();
        });

    }

    public synchronized void close(){
        currentTransaction.close();
    }

    /**
     * Sets the txs value. If this causes the current fdb transaction to exceed its limitations, then it will be added to a new fdb tx.
     * @param key
     * @param value
     */
    public synchronized void set(byte[] key, byte[] value){
        unsavedKeys.add(key);

        //if we don't have enough space in this tx, open the next. They'll always be enough space in an empty transaction since the max value size < max transaction size.
        //Key lengths are multiplied as we may save an additional reference to that key to control atomicity
        if(value.length + (key.length * 3) > maxTransactionValueSizeBytes - currentTransactionValueSizeBytes){
            next(true);
        }

        currentTransaction.set(key, value);
        currentTransactionValueSizeBytes += value.length + (key.length * 2);
    }

    /**
     * Clears the key and it's corresponding data. If this causes the current fdb transaction to exceed its limitations, then it will be added to a new fdb tx.
     * @param key The key of the item you want to clear
     */
    public synchronized void clear(byte[] key){
        unsavedKeys.add(key);

        if(key.length * 3 > maxTransactionValueSizeBytes - currentTransactionValueSizeBytes){
            next(true);
        }

        currentTransactionValueSizeBytes += (key.length * 3);
        currentTransaction.clear(key);
    }

    /**
     * Clears the key and th. If the transaction does not complete, then it will not save any references to be able to recover
     */
    public synchronized void clearReferenceData(byte[] referenceKey, byte[] key){
        if(key.length * 2 > maxTransactionValueSizeBytes - currentTransactionValueSizeBytes){
            next(false);
        }

        currentTransaction.clear(referenceKey); // the reference
        currentTransaction.clear(key); // the data
    }


    public synchronized void clear(Range range){
        currentTransaction.clear(range);
    }

    public synchronized CompletableFuture<byte[]> get(byte[] key){
        return currentTransaction.get(key);
    }

    public synchronized AsyncIterable<KeyValue> getRange(Range range){
        return currentTransaction.getRange(range);
    }

    public synchronized AsyncIterable<KeyValue> getRange(byte[] keyPreffix, byte[] bytes) {
        return currentTransaction.getRange(keyPreffix, bytes);
    }
}
