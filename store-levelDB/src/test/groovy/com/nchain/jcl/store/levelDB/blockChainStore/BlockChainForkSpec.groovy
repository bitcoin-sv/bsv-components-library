package com.nchain.jcl.store.levelDB.blockChainStore

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockChainStore.events.ChainForkEvent
import com.nchain.jcl.store.blockChainStore.events.ChainPruneEvent
import com.nchain.jcl.store.levelDB.common.TestingUtils
import spock.lang.Specification

import java.nio.file.Path
import java.time.Duration

/**
 * Test scenarios involving a Fork and Prune operations
 */
class BlockChainForkSpec extends Specification {

    /**
     * We create a MAin Chian, and then a Fork Chain. We check that the Fork event is triggered. Then we prune the
     * fork chain, and we check that the chain is pruned and the event triggered.
     */
    def "Testing Fork and Prunning"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockHeader block_genesis = TestingUtils.buildBlock()
            println(" - Genesis Block: " + block_genesis.getHash())
            BlockChainStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig.chainBuild()
                .workingFolder(dbPath)
                .genesisBlock(block_genesis)
                .build()
            BlockChainStore db = BlockChainStoreLevelDB.chainBuilder()
                    .config(dbConfig)
                    .build()

            // We keep track of the Events triggered:
            List<ChainForkEvent> forkEvents = new ArrayList<>()
            List<ChainPruneEvent> pruneEvents = new ArrayList<>()

            db.EVENTS().FORKS.forEach({ e -> forkEvents.add(e)})
            db.EVENTS().PRUNINGS.forEach({e -> pruneEvents.add(e)})

        when:
            db.start()

            // We insert the main Chain: [genesis] -> 1 -> 2 -> 3 -> 4

            BlockHeader block_1 = TestingUtils.buildBlock(block_genesis.getHash().toString())
            BlockHeader block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            BlockHeader block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            BlockHeader block_4 = TestingUtils.buildBlock(block_3.getHash().toString())
            println(" - Block 1: " + block_1.getHash())
            println(" - Block 2: " + block_2.getHash())
            println(" - Block 3: " + block_3.getHash())
            println(" - Block 4: " + block_4.getHash())
            db.saveBlocks(Arrays.asList(block_1, block_2, block_3, block_4))

            // We insert a FORK Branch:
            // [genesis] -> 1 -> 2 -> 3  -> 4
            //                    \-> 3B -> 4B

            BlockHeader block_3B = TestingUtils.buildBlock(block_2.getHash().toString())
            BlockHeader block_4B = TestingUtils.buildBlock(block_3B.getHash().toString())
            println(" - Block 3B: " + block_3B.getHash())
            println(" - Block 4B: " + block_4B.getHash())
            db.saveBlocks(Arrays.asList(block_3B, block_4B))


            // We verify the Tips fo the Chains:
            List<Sha256Wrapper> tips = db.getTipsChains()
            boolean tipsOK = (tips.size() == 2) && (tips.contains(block_4.getHash())) && (tips.contains(block_4B.getHash()))

            // and now we prune it:
            db.prune(block_4B.getHash(), false)

            // Now we check the fork chain is gone:
            tips = db.getTipsChains()
            boolean afterPrune = (tips.size() == 1) && (tips.get(0).equals(block_4.getHash()))

            // Some waiting, to give the events time to reach the callbacks:
            Thread.sleep(50)

            db.stop()

        then:
            tipsOK
            pruneEvents.size() == 1
            pruneEvents.get(0).tipForkHash.equals(block_4B.getHash())
            pruneEvents.get(0).parentForkHash.equals(block_2.getHash())
            pruneEvents.get(0).numBlocksPruned == 2
            forkEvents.size() == 1
            forkEvents.get(0).blockForkHash.equals(block_3B.getHash())
            forkEvents.get(0).parentForkHash.equals(block_2.getHash())
            afterPrune

        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }


    /**
     * We test the Automatic Prunning is working as expected, triggering the prunning and removing the Forks that
     * meet the criteria to be pruned.
     */
    def "Testing automatic Prunning"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockHeader block_genesis = TestingUtils.buildBlock()
            println(" - Genesis Block: " + block_genesis.getHash())
            BlockChainStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig.chainBuild()
                    .workingFolder(dbPath)
                    .genesisBlock(block_genesis)
                    .build()
            // We activate Automatic Prunning:
            BlockChainStore db = BlockChainStoreLevelDB.chainBuilder()
                    .config(dbConfig)
                    .enableAutomaticPrunning(true)
                    .prunningFrequency(Duration.ofSeconds(1))
                    .prunningHeightDifference(1)
                    .build()

            // We keep track of the Events triggered:
            List<ChainForkEvent> forkEvents = new ArrayList<>()
            List<ChainPruneEvent> pruneEvents = new ArrayList<>()

            db.EVENTS().FORKS.forEach({ e -> forkEvents.add(e)})
            db.EVENTS().PRUNINGS.forEach({e -> pruneEvents.add(e)})

        when:
            db.start()

            // We insert the main Chain: [genesis] -> 1 -> 2 -> 3 -> 4

            BlockHeader block_1 = TestingUtils.buildBlock(block_genesis.getHash().toString())
            BlockHeader block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            BlockHeader block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            BlockHeader block_4 = TestingUtils.buildBlock(block_3.getHash().toString())
            println(" - Block 1: " + block_1.getHash())
            println(" - Block 2: " + block_2.getHash())
            println(" - Block 3: " + block_3.getHash())
            println(" - Block 4: " + block_4.getHash())
            db.saveBlocks(Arrays.asList(block_1, block_2, block_3, block_4))

            // We insert several FORKS:
            // [genesis] -> 1 -> 2  -> 3  -> 4
            //                     \-> 3B
            //               \-> 2C -> 3C

            BlockHeader block_3B = TestingUtils.buildBlock(block_2.getHash().toString())
            BlockHeader block_2C = TestingUtils.buildBlock(block_1.getHash().toString())
            BlockHeader block_3C = TestingUtils.buildBlock(block_2C.getHash().toString())
            println(" - Block 3B: " + block_3B.getHash())
            println(" - Block 2C: " + block_2C.getHash())
            println(" - Block 3C: " + block_3C.getHash())

            // We insert the blocks separately so we can make sure in this test that the FORK events are also
            // triggered in the same order.
            db.saveBlock(block_3B)
            Thread.sleep(500)
            db.saveBlock(block_2C)
            db.saveBlock(block_3C)

            // We wait a bit, but NOT enough to trigger the Automatic Prune:
            Thread.sleep(2000)

            // Now we wait for long enough for the Automatic Prune to happen
            Thread.sleep(4000)

            // Now we check the state of the Tips of the chain:
            List<Sha256Wrapper> tips = db.getTipsChains()
            boolean tipChainAfterPruneOK = (tips.size() == 1) && (tips.get(0).equals(block_4.getHash()))

            // Now we check the content of the PRUNE Events. When there are more than one FORK like here, we cannot
            // assume the order of the PRUNE EVENTS being launched, so we check the content of each one:

            boolean pruneEventsOK = true
            for (ChainPruneEvent pruneEvent : pruneEvents) {
                if (pruneEvent.tipForkHash.equals(block_3B.getHash())) {
                    pruneEventsOK &= pruneEvent.parentForkHash.equals(block_2.getHash()) && pruneEvent.numBlocksPruned == 1
                } else {
                    pruneEventsOK &= pruneEvent.parentForkHash.equals(block_1.getHash()) && pruneEvent.numBlocksPruned == 2
                }
            }

            // Some waiting, to give the events time to reach the callbacks:
            Thread.sleep(50)

            db.stop()

        then:

            forkEvents.size() == 2
            pruneEvents.size() == 2

            // The first FORK Event is for saving Block_3B:
            forkEvents.get(0).blockForkHash.equals(block_3B.getHash())
            forkEvents.get(0).parentForkHash.equals(block_2.getHash())

            // The second FORK Event is for saving Block_2C:
            forkEvents.get(1).blockForkHash.equals(block_2C.getHash())
            forkEvents.get(1).parentForkHash.equals(block_1.getHash())

            // Contents of the PRUNE Events and state of the main chain afterwards:
            pruneEventsOK
            tipChainAfterPruneOK

        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }
}
