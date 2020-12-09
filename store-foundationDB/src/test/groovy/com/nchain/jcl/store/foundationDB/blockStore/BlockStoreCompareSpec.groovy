package com.nchain.jcl.store.foundationDB.blockStore

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockStore.BlocksCompareResult
import com.nchain.jcl.store.foundationDB.common.TestingUtils
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Testig class for scenarios related to the blocks Comparison Scenario
 */
class BlockStoreCompareSpec extends Specification {
    /**
     * We test 2 custom dummy Blocks, crated from scratch
     */
    def "Testing 2 custom Blocks"() {
        final int NUM_TXS           = 4      // Txs in each Block
        final int NUM_TXS_IN_COMMON = 2      // Txs to copy from one Block to another
        given:
            println(" - Connecting to the DB...")
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .triggerBlockEvents(true)
                    .triggerTxEvents(true)
                    .build()
        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)
            // We create a Block A, and link some Txs to it:
            BlockHeader blockA = TestingUtils.buildBlock()
            blockStore.saveBlock(blockA)
            List<Tx> txsA = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txsA.add(TestingUtils.buildTx())
            blockStore.saveBlockTxs(blockA.getHash(), txsA)


            // We create a Block B, and link some Txs to it:
            BlockHeader blockB = TestingUtils.buildBlock()
            blockStore.saveBlock(blockB)
            List<Tx> txsB = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txsB.add(TestingUtils.buildTx())
            blockStore.saveBlockTxs(blockB.getHash(), txsB)

            // We create and save some Txs in common:
            List<Tx> txsInCommon = new ArrayList<>()
            for (int i = 0; i < NUM_TXS_IN_COMMON; i++) {
                Tx tx = TestingUtils.buildTx()
                txsInCommon.add(tx)
                blockStore.saveTx(tx)
                blockStore.linkTxToBlock(tx.getHash(), blockA.getHash())
                blockStore.linkTxToBlock(tx.getHash(), blockB.getHash())
            }

            blockStore.printKeys()

            // Now we compare both blocks:
            BlocksCompareResult comparison = blockStore.compareBlocks(blockA.getHash(), blockB.getHash()).get()

            // We check the Txs in Common:
            println("> Checking TXs in Common:")
            Instant timeInit = Instant.now()
            AtomicBoolean inCommonOK = new AtomicBoolean(true)
            Iterator<Sha256Wrapper> txsInCommonIt = comparison.getTxsInCommonIt().iterator()
            long numTxsInCommon = 0;
            while (txsInCommonIt.hasNext()) {
                Sha256Wrapper txHash = txsInCommonIt.next();
                numTxsInCommon++;
                println("  - TX in common: " + txHash)
                inCommonOK.set(inCommonOK.get() && blockStore.isTxLinkToblock(txHash, blockA.getHash()))
                inCommonOK.set(inCommonOK.get() && blockStore.isTxLinkToblock(txHash, blockB.getHash()))
            }
            println("  - " + numTxsInCommon + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")

            // We check the TXS that are ONLY in the Block A:
            println("> Checking TXs only in block " + blockA.getHash() + ":")
            timeInit = Instant.now()
            AtomicBoolean onlyAOK = new AtomicBoolean(true)
            Iterator<Sha256Wrapper> txsOnlyA = comparison.getTxsOnlyInA().iterator()
            long numTxsOnlyInA = 0;
            while (txsOnlyA.hasNext()) {
                Sha256Wrapper txHash = txsOnlyA.next();
                numTxsOnlyInA++
                println("  - TX only in Block A: " + txHash)
                onlyAOK.set(onlyAOK.get() && blockStore.isTxLinkToblock(txHash, blockA.getHash()) && !blockStore.isTxLinkToblock(txHash, blockB.getHash()))
            }
            println("  - " + numTxsOnlyInA + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")

            // We check the TXS that are ONLY in the Block B:
            println("> Checking TXs only in block " + blockB.getHash() + ":")
            timeInit = Instant.now()
            AtomicBoolean onlyBOK = new AtomicBoolean(true)
            Iterator<Sha256Wrapper> txsOnlyB = comparison.getTxsOnlyInB().iterator()
            long numTxsOnlyInB = 0;
            while (txsOnlyB.hasNext()) {
                Sha256Wrapper txHash = txsOnlyB.next();
                numTxsOnlyInB++
                println("  - TX only in Block B: " + txHash)
                onlyBOK.set(onlyBOK.get() && blockStore.isTxLinkToblock(txHash, blockB.getHash()) && !blockStore.isTxLinkToblock(txHash, blockA.getHash()))
            }
            println("  - " + numTxsOnlyInB + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")


        then:
            inCommonOK.get()
            onlyAOK.get()
            onlyBOK.get()
        cleanup:
            blockStore.removeBlockTxs(blockA.getHash())
            blockStore.removeBlockTxs(blockB.getHash())
            blockStore.printKeys()
            blockStore.stop()
    }
}
