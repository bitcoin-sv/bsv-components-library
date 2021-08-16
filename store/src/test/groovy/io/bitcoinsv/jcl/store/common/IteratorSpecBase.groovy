/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.common


import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import io.bitcoinj.bitcoin.api.base.Tx
import io.bitcoinj.core.Sha256Hash
import io.bitcoinsv.jcl.store.blockStore.BlockStoreSpecBase

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Predicate

/**
 * Testing iterator for the FBDIterator, which is a very basic class and plays a big role in the FoundationDB
 * implementation of the JCL-Store.
 *
 * This class can NOT be tested itself, it needs to be extended. An extending class must implement the "getInstance"
 * method, which returns a concrete implementation of the BlockStore interface (like a LevelDB or FoundationDB
 * Implementation). The extendins class must also implement the "createIteratorForTxs" methods, which returns an
 * implementaion mof the Iterator<String> interface.
 *
 * Once that method is implemented, the extending class can be tested without any other additions, since running the
 * extending class will automatically trigger the tests defined in THIS class.
 *
 * All the methods in thi class behave ina similar way: A block wiht some Txs linked to it is saved, and then an
 * iterator is created with different configurations, and tested.
 */
abstract class IteratorSpecBase extends BlockStoreSpecBase {

    /** Number of TXs linked to the Block inserted in the DB */
    static final int NUM_TXS = 2

    static final String PREFFIX_TX_ALL      = "tx"
    static final String PREFFIX_TX_BLOCK    = "tx_block_link:"

    /** Returns an instance of a Iterator that iterates over the Txs and returns the Key in String format */
    abstract Iterator<byte[]> createIteratorForTxs(BlockStore db, String preffix, String suffix)

    /** Indicates if a Key starts with the preffix given */
    boolean keyStartsWith(byte[] key, byte[] preffix) {
        //println("Checking Key: " + Arrays.toString(key) + ", starting with: " + Arrays.toString(preffix))
        for (int i = 0; i < preffix.length; i++)
            if (preffix[i] != key[i]) return false;
        return true;
    }

    boolean keyStartsWith(byte[] key, String preffix) { return keyStartsWith(key, preffix.getBytes())}

    /** Indicates if a Key ends with the suffix given */
    boolean keyEndsWith(byte[] key, byte[] suffix) {
        for (int i = 1; i <= suffix.length; i++)
            if (suffix[suffix.length -i ] != key[key.length -i]) return false;
        return true;
    }

    boolean keyEndsWith(byte[] key, String suffix) { return keyEndsWith(key, suffix.getBytes())}

    /**
     * Convenience method to insert a block and several Txs in the DB and return the list of Tx Hashes inserted
     */
    private List<Sha256Hash> loadInitData(HeaderReadOnly block, BlockStore blockStore) {
        blockStore.saveBlock(block)
        List<Sha256Hash> result = new ArrayList<>()
        for (int i = 0; i < NUM_TXS; i++) {
            Tx tx = TestingUtils.buildTx()
            result.add(tx.getHash())
            blockStore.saveTx(tx)
            blockStore.linkTxToBlock(tx.getHash(), block.getHash())
        }
        Thread.sleep(200)
        return result;
    }

    /**
     * Convenience method to check if the iterator traverse through the right keys
     */
    private boolean checkIterator(Iterator<byte[]> it, Predicate<byte[]> checkKey) {
        AtomicLong numKeys = new AtomicLong()
        AtomicBoolean result = new AtomicBoolean(true)
        while (it.hasNext()) {
            byte[] key = it.next()
            numKeys.incrementAndGet()
            boolean isKeyValid = checkKey.test(key)
            println(" - Reading Key " + "(" + isKeyValid + ") " + new String(key) + ", " + Arrays.toString(key))
            if (!isKeyValid) result.set(false)
        }
        return ((numKeys.get() > 0) && result.get())
    }

    /**
     * We store some Txs in a Directory, and we test that the iterator traverse all of them.
     * IMPORTANT: For each Tx, several Keys are Stored:
     *  - "tx:[block_hash]
     *  - "tx_p:[block_hash]:[property] -> 2 property (":txsNeeded", ":blocks")
     *
     */
    def "testing traversing the whole content of Directory"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, false)

        when:
            db.start()
            //TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            HeaderReadOnly block = TestingUtils.buildBlock()
            List<Sha256Hash> txHashes = loadInitData(block, db)
            db.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            Iterator<byte[]> it = createIteratorForTxs(db, null, null)
            println(" - Checking iterator...")
            OK.set(checkIterator(it, {k -> keyStartsWith(k, PREFFIX_TX_ALL)}))

        then:
            OK.get()
        cleanup:
            db.removeBlock(block.getHash())
            db.removeTxs(txHashes)
            db.printKeys()
            db.stop()
    }

    /**
     * We store some Keys in a Directory, and we test that the iterator only traverses those ones that start with a
     * specific preffix ("b_p:").
     * IMPORTANT: For each Block, several Keys are Stored:
     *  - "tx:[block_hash]
     *  - "tx_p:[block_hash]:[property] -> 2 property (":txsNeeded", ":blocks")
     *
     */
    def "testing traversing a directory, only keys starting with a Preffix"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, false)

        when:
            db.start()
            //TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            HeaderReadOnly block = TestingUtils.buildBlock()
            List<Sha256Hash> txHashes = loadInitData(block, db)
            db.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            Iterator<byte[]> it = createIteratorForTxs(db, PREFFIX_TX_BLOCK, null)
            println(" - Checking iterator...")
            OK.set(checkIterator(it, {k -> keyStartsWith(k, PREFFIX_TX_BLOCK)}))

        then:
            OK.get()
        cleanup:
            db.removeBlock(block.getHash())
            db.removeTxs(txHashes)
            db.printKeys()
            db.stop()
    }

    /**
     * We store some Keys in a Directory, and we test that the iterator only traverse those ones that start with a
     * specific preffix and also ends with a specific suffix.
     * IMPORTANT: For each Block, several Keys are Stored:
     *  - "tx:[block_hash]
     *  - "tx_p:[block_hash]:[property] -> 2 property (":txsNeeded", ":blocks")
     *
     */
    def "testing traversing a directory, only keys starting with a prefix and ending with a suffix"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, false)

        when:
            db.start()
            //TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            HeaderReadOnly block = TestingUtils.buildBlock()
            List<Sha256Hash> txHashes = loadInitData(block, db)
            db.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            String suffix = ":" + block.getHash().toString() + ":"
            Iterator<byte[]> it = createIteratorForTxs(db, PREFFIX_TX_BLOCK, suffix)
            println(" - Checking iterator...")
            OK.set(checkIterator(it, {k -> (keyStartsWith(k, PREFFIX_TX_BLOCK) && keyEndsWith(k, suffix))}))

        then:
            OK.get()
        cleanup:
            db.removeBlock(block.getHash())
            db.removeTxs(txHashes)
            db.printKeys()
            db.stop()
    }

    /**
     * We store some Keys in a Directory, and we test that the iterator only traverse those ones that ends with a
     * specific suffix.
     * IMPORTANT: For each Block, several Keys are Stored:
     *  - "tx:[block_hash]
     *  - "tx_p:[block_hash]:[property] -> 2 property (":txsNeeded", ":blocks")
     *
     */
    def "testing traversing a directory, only keys ending with a suffix"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, false)

        when:
            db.start()
            //TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            HeaderReadOnly block = TestingUtils.buildBlock()
            List<Sha256Hash> txHashes = loadInitData(block, db)
            db.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            String suffix = ":" + block.getHash().toString() + ":"
            Iterator<byte[]> it = createIteratorForTxs(db, null, suffix)
            println(" - Checking iterator...")
            OK.set(checkIterator(it, {k -> keyEndsWith(k, suffix)}))

        then:
            OK.get()
        cleanup:
            db.removeBlock(block.getHash())
            db.removeTxs(txHashes)
            db.printKeys()
            db.stop()
    }

}
