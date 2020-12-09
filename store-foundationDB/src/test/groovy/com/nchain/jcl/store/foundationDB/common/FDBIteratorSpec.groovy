package com.nchain.jcl.store.foundationDB.common

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDB
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

/**
 * Testing iterator for the FBDIterator, which is a very basic class and plays a big role in the FoundationDB
 * implementation of the JCL-Store
 */
class FDBIteratorSpec extends Specification {

    static final int NUM_TXS = 2

    /**
     * Convenience method to insert several Tx in the DB and return the list of Tx Hashes inserted
     */
    private List<Sha256Wrapper> insertBlockAndTxs(BlockHeader block, BlockStore blockStore) {
        blockStore.saveBlock(block)
        List<Sha256Wrapper> result = new ArrayList<>()
        for (int i = 0; i < NUM_TXS; i++) {
            Tx tx = TestingUtils.buildTx()
            result.add(tx.getHash())
            blockStore.saveTx(tx)
            blockStore.linkTxToBlock(tx.getHash(), block.getHash())
        }
        return result;
    }

    /**
     * Convenience method to check if the iterator traverse through the right keys
     */
    private boolean checkIterator(Iterator<String> it, Predicate<String> checkKey) {
        AtomicBoolean result = new AtomicBoolean(true)
        while (it.hasNext()) {
            String key = it.next()
            println(" - Reading Key " + KeyValueUtils.cleanKey(key).get())
            boolean isKeyValid = checkKey.test(KeyValueUtils.cleanKey(key).get())
            if (!isKeyValid) result.set(false)
        }
        return result.get()
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
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .build()
        when:
            blockStore.start()
            TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            BlockHeader block = TestingUtils.buildBlock()
            List<Sha256Wrapper> txHashes = insertBlockAndTxs(block, blockStore)
            blockStore.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            blockStore.db.run({ tr ->
                Iterator<String> it = FDBIterator.<String>iteratorBuilder()
                        .transaction(tr)
                        .fromDir(blockStore.getTxsDir())
                        .buildItemBy({kv -> new String(kv.key)})
                        .build()

                OK.set(checkIterator(it, {k -> k.startsWith("tx")}))
                return null
            })
        then:
            OK.get()
        cleanup:
            blockStore.removeBlock(block.getHash())
            blockStore.removeTxs(txHashes)
            blockStore.printKeys()
            blockStore.stop()
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
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .build()
        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            BlockHeader block = TestingUtils.buildBlock()
            List<Sha256Wrapper> txHashes = insertBlockAndTxs(block, blockStore)
            blockStore.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            blockStore.db.run({ tr ->
                Iterator<String> it = FDBIterator.<String>iteratorBuilder()
                        .transaction(tr)
                        .fromDir(blockStore.getTxsDir())
                        .startingWithPreffix(KeyValueUtils.bytes(KeyValueUtils.KEY_PREFFIX_TX_PROP))
                        .buildItemBy({kv -> new String(kv.key)})
                        .build()

                OK.set(checkIterator(it, {k -> k.startsWith(KeyValueUtils.KEY_PREFFIX_TX_PROP)}))
                return null
            })
        then:
            OK.get()
        cleanup:
            blockStore.removeBlock(block.getHash())
            blockStore.removeTxs(txHashes)
            blockStore.printKeys()
            blockStore.stop()
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
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .build()
        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            BlockHeader block = TestingUtils.buildBlock()
            List<Sha256Wrapper> txHashes = insertBlockAndTxs(block, blockStore)
            blockStore.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            blockStore.db.run({ tr ->
                Iterator<String> it = FDBIterator.<String>iteratorBuilder()
                        .transaction(tr)
                        .fromDir(blockStore.getTxsDir())
                        .startingWithPreffix(KeyValueUtils.bytes(KeyValueUtils.KEY_PREFFIX_TX_PROP))
                        .endingWithSuffix(KeyValueUtils.bytes(KeyValueUtils.KEY_SUFFIX_TX_BLOCKS))
                        .buildItemBy({kv -> new String(kv.key)})
                        .build()

                OK.set(checkIterator(it, {k ->
                    (k.startsWith(KeyValueUtils.KEY_PREFFIX_TX_PROP) &&
                    k.endsWith(KeyValueUtils.KEY_SUFFIX_TX_BLOCKS))
                }))
                return null
            })
        then:
            OK.get()
        cleanup:
            blockStore.removeBlock(block.getHash())
            blockStore.removeTxs(txHashes)
            blockStore.printKeys()
            blockStore.stop()
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
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .build()
        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)

            // We insert a Block and several Txs linked to it:
            BlockHeader block = TestingUtils.buildBlock()
            List<Sha256Wrapper> txHashes = insertBlockAndTxs(block, blockStore)
            blockStore.printKeys()
            AtomicBoolean OK = new AtomicBoolean(true)

            // Now we create an Iterator and iterate over the Keys:
            blockStore.db.run({ tr ->
                Iterator<String> it = FDBIterator.<String>iteratorBuilder()
                        .transaction(tr)
                        .fromDir(blockStore.getTxsDir())
                        .endingWithSuffix(KeyValueUtils.bytes(KeyValueUtils.KEY_SUFFIX_TX_BLOCKS))
                        .buildItemBy({kv -> new String(kv.key)})
                        .build()

                OK.set(checkIterator(it, {k -> k.endsWith(KeyValueUtils.KEY_SUFFIX_TX_BLOCKS)}))
                return null
            })
        then:
            OK.get()
        cleanup:
            blockStore.removeBlock(block.getHash())
            blockStore.removeTxs(txHashes)
            blockStore.printKeys()
            blockStore.stop()
    }


}
