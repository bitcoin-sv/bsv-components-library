package com.nchain.jcl.store.blockStore


import com.nchain.jcl.store.common.TestingUtils
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Testig class for scenarios related to the blocks Comparison Scenario
 */
abstract class BlockStoreCompareSpecBase extends BlockStoreSpecBase {

    /**
     * We test 2 custom dummy Blocks, created from scratch, and link some Txs to each of them, and some Txs linked to
     * BOTH  of them.
     */
    def "Testing 2 custom Blocks"() {
        final int NUM_TXS           = 4      // Txs in each Block
        final int NUM_TXS_IN_COMMON = 2      // Txs to copy from one Block to another
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, false)
        when:
            db.start()

            // We create a Block A, and link some Txs to it:
            HeaderReadOnly blockA = TestingUtils.buildBlock()
            db.saveBlock(blockA)
            List<Tx> txsA = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txsA.add(TestingUtils.buildTx())
            println(" - Saving Block " + blockA.getHash().toString() + " with " + NUM_TXS + " txs linked:")
            txsA.forEach({h -> println(" - tx " + h.getHash().toString())})
            db.saveBlockTxs(blockA.getHash(), txsA)


            // We create a Block B, and link some Txs to it:
            HeaderReadOnly blockB = TestingUtils.buildBlock()
            db.saveBlock(blockB)
            List<Tx> txsB = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txsB.add(TestingUtils.buildTx())
            println(" - Saving Block " + blockB.getHash().toString() + " with " + NUM_TXS + " txs linked:")
            txsB.forEach({h -> println(" - tx " + h.getHash().toString())})
            db.saveBlockTxs(blockB.getHash(), txsB)

            // We create and save some Txs in common:
            println(" - Saving TXs that will be linked to both Blocks...")
            List<Tx> txsInCommon = new ArrayList<>()
            for (int i = 0; i < NUM_TXS_IN_COMMON; i++) {
                Tx tx = TestingUtils.buildTx()
                txsInCommon.add(tx)
                println(" - Saving Tx " + tx.getHash().toString() + " and linked to both Blocks...")
                db.saveTx(tx)
                db.linkTxToBlock(tx.getHash(), blockA.getHash())
                db.linkTxToBlock(tx.getHash(), blockB.getHash())
            }

            // We check the DB Content in the console...
            db.printKeys()

            // Now we compare both blocks:
            BlocksCompareResult comparison = db.compareBlocks(blockA.getHash(), blockB.getHash()).get()

            // We check the Txs in Common:
            println(" - Checking TXs in Common:")
            Instant timeInit = Instant.now()
            AtomicBoolean inCommonOK = new AtomicBoolean(true)
            Iterator<Sha256Hash> txsInCommonIt = comparison.getTxsInCommonIt().iterator()
            long numTxsInCommon = 0;
            while (txsInCommonIt.hasNext()) {
                Sha256Hash txHash = txsInCommonIt.next();
                numTxsInCommon++;
                println("  - TX in common: " + txHash)
                inCommonOK.set(inCommonOK.get() && db.isTxLinkToblock(txHash, blockA.getHash()))
                inCommonOK.set(inCommonOK.get() && db.isTxLinkToblock(txHash, blockB.getHash()))
            }
            println(" - " + numTxsInCommon + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")

            // We check the TXS that are ONLY in the Block A:
            println(" - Checking TXs only in block " + blockA.getHash() + ":")
            timeInit = Instant.now()
            AtomicBoolean onlyAOK = new AtomicBoolean(true)
            Iterator<Sha256Hash> txsOnlyA = comparison.getTxsOnlyInA().iterator()
            long numTxsOnlyInA = 0;
            while (txsOnlyA.hasNext()) {
                Sha256Hash txHash = txsOnlyA.next();
                numTxsOnlyInA++
                println(" - TX only in Block A: " + txHash)
                onlyAOK.set(onlyAOK.get() && db.isTxLinkToblock(txHash, blockA.getHash()) && !db.isTxLinkToblock(txHash, blockB.getHash()))
            }
            println(" - " + numTxsOnlyInA + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")

            // We check the TXS that are ONLY in the Block B:
            println(" - Checking TXs only in block " + blockB.getHash() + ":")
            timeInit = Instant.now()
            AtomicBoolean onlyBOK = new AtomicBoolean(true)
            Iterator<Sha256Hash> txsOnlyB = comparison.getTxsOnlyInB().iterator()
            long numTxsOnlyInB = 0;
            while (txsOnlyB.hasNext()) {
                Sha256Hash txHash = txsOnlyB.next();
                numTxsOnlyInB++
                println(" - TX only in Block B: " + txHash)
                onlyBOK.set(onlyBOK.get() && db.isTxLinkToblock(txHash, blockB.getHash()) && !db.isTxLinkToblock(txHash, blockA.getHash()))
            }
            println(" - " + numTxsOnlyInB + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")


        then:
            inCommonOK.get()
            onlyAOK.get()
            onlyBOK.get()
        cleanup:
            println(" - Cleanup...")
            db.removeBlockTxs(blockA.getHash())
            db.removeBlockTxs(blockB.getHash())
            db.removeBlock(blockA.getHash())
            db.removeBlock(blockB.getHash())
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }
}
