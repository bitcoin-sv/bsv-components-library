package io.bitcoinsv.jcl.store.blockChainStore


import io.bitcoinsv.jcl.store.common.TestingUtils
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import io.bitcoinj.core.Sha256Hash

import java.time.Duration

/**
 * Testing class for different scenarios where the whole DB is cleared.
 */
abstract class BlockChainClearDBSpecBase extends BlockChainStoreSpecBase {

    /**
     * We test that the Db is properly cleared after some data has been stored.
     */
    def "testing clearing DB with some little data"() {
        final int NUM_BLOCKS = 2
        final int NUM_TXS = 2
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

        when:
            db.start()
            // We define a series of Blocks and Txs and save them:
            println("Inserting " + NUM_BLOCKS + " blocks and " + NUM_TXS + " Txs...")
            for (int i = 0; i < NUM_BLOCKS; i++) db.saveBlock(TestingUtils.buildBlock())
            for (int i = 0; i < NUM_TXS; i++) db.saveTx(TestingUtils.buildTx())

            int numBlocksBeforeClearing = db.getNumBlocks()
            int numTxsBeforeClearing = db.getNumTxs()

            // We check the DB Content in the console...
            db.printKeys()

            // Now we CLEAR the DB
            println("Clearing the DB...")
            db.clear()

            // And we check that the DB is actually Empty:
            int numBlocksAfterClearing = db.getNumBlocks()
            int numTxsAfterClearing = db.getNumTxs()

            // We check the DB Content in the console...
            db.printKeys()

            // Just in case, now we insert again, to check that the DB has come back to a normal state and
            // it can keep working as usual...
            println("Re-Inserting " + NUM_BLOCKS + " blocks and " + NUM_TXS + " Txs...")
            for (int i = 0; i < NUM_BLOCKS; i++) db.saveBlock(TestingUtils.buildBlock())
            for (int i = 0; i < NUM_TXS; i++) db.saveTx(TestingUtils.buildTx())

            int numBlocksAfterResinserting = db.getNumBlocks()
            int numTxsAfterReinserting = db.getNumTxs()

            // We check the DB Content in the console...
            db.printKeys()

            // And we finally clear it again:
            println("Clearing the DB for the last time...")
            db.clear()

        then:
            numBlocksBeforeClearing == NUM_BLOCKS + 1
            numTxsBeforeClearing == NUM_TXS
            numBlocksAfterClearing == 1
            numTxsAfterClearing == 0
            numBlocksAfterResinserting == NUM_BLOCKS + 1
            numTxsAfterReinserting == NUM_TXS

        cleanup:
            println(" - Cleanup...")
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }
}
