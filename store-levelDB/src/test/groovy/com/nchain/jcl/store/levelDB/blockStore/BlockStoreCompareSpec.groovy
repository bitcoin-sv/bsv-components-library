package com.nchain.jcl.store.levelDB.blockStore

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlocksCompareResult
import com.nchain.jcl.store.levelDB.common.TestingUtils
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Testig class for scenarios related to the blocks Comparison Scenario
 */
@Ignore
class BlockStoreCompareSpec extends Specification {

    /**
     * We test 2 custom dummy Blocks, crated from scratch
     */
    def "Testing 2 custom Blocks"() {
        final int NUM_TXS = 500_000             // Txs in each Block
        final int NUM_TXS_IN_COMMON = 5000      // Txs to copy from one Block to another
        final int NUM_TXS_BATCH = 10000         // Number of TXs saved in each Batch...
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .workingFolder(dbPath)
                    .build()
            BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerTxEvents(true)
                    .build()
        when:
            db.start()
            // We create a Block A, and link some Txs to it:
            BlockHeader blockA = TestingUtils.buildBlock();
            db.saveBlock(blockA);

            List<Tx> txsA = new ArrayList<>()
            int numTotalTxsA = 0
            for (int i = 0; i < NUM_TXS; i++) {
                txsA.add(TestingUtils.buildTx())
                if (txsA.size() % NUM_TXS_BATCH == 0) {
                    numTotalTxsA += txsA.size()
                    db.saveBlockTxs(blockA.getHash(), txsA)
                    println(numTotalTxsA + " Txs saved and linked to " + blockA.getHash() + "...")
                    txsA.clear()
                }
            }

            // We create a Block B, and link some Txs to it:
            BlockHeader blockB = TestingUtils.buildBlock();
            db.saveBlock(blockB);
            List<Tx> txsB = new ArrayList<>()
            int numTotalTxsB = 0
            for (int i = 0; i < NUM_TXS; i++) {
                txsB.add(TestingUtils.buildTx())
                if (txsB.size() % NUM_TXS_BATCH == 0) {
                    numTotalTxsB += txsB.size()
                    db.saveBlockTxs(blockB.getHash(), txsB)
                    println(numTotalTxsB + " Txs saved and linked to " + blockB.getHash() + "...")
                    txsB.clear()
                }
            }

            // We create and save some Txs in common:

            for (int i = 0; i < NUM_TXS_IN_COMMON; i++) {
                Tx tx = TestingUtils.buildTx()
                db.saveTx(tx)
                db.linkTxToBlock(tx.getHash(), blockA.getHash())
                db.linkTxToBlock(tx.getHash(), blockB.getHash())
            }

            //db.printKeys()

            // Now we compare both blocks:
            BlocksCompareResult comparison = db.compareBlocks(blockA.getHash(), blockB.getHash()).get()

            // And we check the Result:

            // We check the Txs in Common:
            println("> Checking TXs in Common:")
            Instant timeInit = Instant.now()
            AtomicBoolean inCommonOK = new AtomicBoolean()
            Iterator<Sha256Wrapper> txsInCommon = comparison.getTxsInCommonIt().iterator()
            long numTxsInCommon = 0;
            while (txsInCommon.hasNext()) {
                Sha256Wrapper txHash = txsInCommon.next();
                numTxsInCommon++;
                //println("  - Checking TX " + txHash)
                inCommonOK.set(db.isTxLinkToblock(txHash, blockA.getHash()))
                inCommonOK.set(db.isTxLinkToblock(txHash, blockB.getHash()))
            }
            println("  - " + numTxsInCommon + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")

            // We check the TXS that are ONLY in the Block A:
            println("> Checking TXs only in block " + blockA.getHash() + ":")
            timeInit = Instant.now()
            AtomicBoolean onlyAOK = new AtomicBoolean()
            Iterator<Sha256Wrapper> txsOnlyA = comparison.getTxsOnlyInA().iterator()
            long numTxsOnlyInA = 0;
            while (txsOnlyA.hasNext()) {
                Sha256Wrapper txHash = txsOnlyA.next();
                numTxsOnlyInA++
                //println("  - Checking TX " + txHash)
                onlyAOK.set(db.isTxLinkToblock(txHash, blockA.getHash()) && !db.isTxLinkToblock(txHash, blockB.getHash()))
            }
            println("  - " + numTxsOnlyInA + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")

            // We check the TXS that are ONLY in the Block B:
            println("> Checking TXs only in block " + blockB.getHash() + ":")
            timeInit = Instant.now()
            AtomicBoolean onlyBOK = new AtomicBoolean()
            Iterator<Sha256Wrapper> txsOnlyB = comparison.getTxsOnlyInB().iterator()
            long numTxsOnlyInB = 0;
            while (txsOnlyB.hasNext()) {
                Sha256Wrapper txHash = txsOnlyB.next();
                numTxsOnlyInB++
                //println("  - Checking TX " + txHash)
                onlyBOK.set(db.isTxLinkToblock(txHash, blockB.getHash()) && !db.isTxLinkToblock(txHash, blockA.getHash()))
            }
            println("  - " + numTxsOnlyInB + " Txs Verified in " + Duration.between(timeInit, Instant.now()).toMillis() + " millisecs")

        db.stop()
        then:
            inCommonOK.get()
            onlyAOK.get()
            onlyBOK.get()
    }
}
