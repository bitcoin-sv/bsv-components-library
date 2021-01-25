package com.nchain.jcl.store.blockChainStore

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockChainStore.events.ChainForkEvent
import com.nchain.jcl.store.blockChainStore.events.ChainPruneEvent
import com.nchain.jcl.store.common.TestingUtils

import java.time.Duration

/**
 * Test scenarios involving a Fork and Prune operations
 */
abstract class BlockChainForkSpecBase extends BlockChainStoreSpecBase {

    /**
     * We create a MAin Chian, and then a Fork Chain. We check that the Fork event is triggered. Then we prune the
     * fork chain, and we check that the chain is pruned and the event triggered.
     */
    def "Testing Fork and Prunning"() {
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            BlockHeader genesisBlock = TestingUtils.buildBlock(Sha256Wrapper.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

            // We keep track of the Events triggered:
            List<ChainForkEvent> forkEvents = new ArrayList<>()
            List<ChainPruneEvent> pruneEvents = new ArrayList<>()

            db.EVENTS().FORKS.forEach({ e ->
                forkEvents.add(e)
                println(" > EVENT DETECTED : Fork : parentBlock: " + e.parentForkHash.toString())
            })
            db.EVENTS().PRUNINGS.forEach({e ->
                pruneEvents.add(e)
                println(" > EVENT DETECTED : Prune : parentBlock: " + e.parentForkHash + " , num Blocks pruned: " + e.numBlocksPruned)
            })

        when:
            db.start()

            // We insert the main Chain: [genesis] -> 1 -> 2 -> 3 -> 4

            BlockHeader block_1 = TestingUtils.buildBlock(genesisBlock.getHash().toString())
            BlockHeader block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            BlockHeader block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            BlockHeader block_4 = TestingUtils.buildBlock(block_3.getHash().toString())

            println(" - Inserting a Chain of 5 blocks:")
            db.saveBlocks(Arrays.asList(block_1, block_2, block_3, block_4))

            println("  Genesis:" + genesisBlock.getHash().toString())
            println("  Block 1:  |- " + block_1.getHash().toString())
            println("  Block 2:       |- " + block_2.getHash().toString())
            println("  Block 3:            |- " + block_3.getHash().toString())
            println("  Block 4:                 |- " + block_4.getHash().toString())

            // We verify the Tips fo the Chains:
            List<Sha256Wrapper> tips = db.getTipsChains()
            println(" - Chain Tips:")
            tips.forEach({ t -> println(" - Block " + t.toString())})

            // We insert a FORK Branch:
            // [genesis] -> 1 -> 2 -> 3  -> 4
            //                    \-> 3B -> 4B

            BlockHeader block_3B = TestingUtils.buildBlock(block_2.getHash().toString())
            BlockHeader block_4B = TestingUtils.buildBlock(block_3B.getHash().toString())
            println(" - Inserting a Chain of 2 Blocks (3B and 4B) on top of Block 2:")
            println("  Genesis:" + genesisBlock.getHash().toString())
            println("  Block 1:  |- " + block_1.getHash().toString())
            println("  Block 2:       |- " + block_2.getHash().toString())
            println("  Block 3:            |- " + block_3.getHash().toString())
            println("  Block 4:                 |- " + block_4.getHash().toString())
            println("  Block 3B:           |- " + block_3B.getHash().toString())
            println("  Block 4B:                |- " + block_4B.getHash().toString())
            db.saveBlocks(Arrays.asList(block_3B, block_4B))

            // We verify the Tips fo the Chains:
            tips = db.getTipsChains()
            println(" - Chain Tips:")
            tips.forEach({ t -> println(" - Block " + t.toString())})

            boolean tipsOK = (tips.size() == 2) && (tips.contains(block_4.getHash())) && (tips.contains(block_4B.getHash()))

            // and now we prune it:
            println(" > Prunning from Tip: " + block_4B.getHash() + " ...")
            db.prune(block_4B.getHash(), false)

            // Now we check the fork chain is gone:
            tips = db.getTipsChains()
            println(" - Chain Tips:")
            tips.forEach({ t -> println(" - Block " + t.toString())})

            boolean afterPrune = (tips.size() == 1) && (tips.get(0).equals(block_4.getHash()))

            // Some waiting, to give the events time to reach the callbacks:
            Thread.sleep(50)

            // We check the DB Content in the console...
            db.printKeys()

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
            println(" - Cleanup...")
            db.removeBlocks(Arrays.asList(genesisBlock.getHash(), block_1.getHash(), block_2.getHash(),
                    block_3.getHash(), block_4.getHash(), block_3B.getHash(), block_4B.getHash()))
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }


    /**
     * We test the Automatic Prunning is working as expected, triggering the prunning and removing the Forks that
     * meet the criteria to be pruned.
     */
    def "Testing automatic Prunning"() {
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            BlockHeader genesisBlock = TestingUtils.buildBlock(Sha256Wrapper.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), Duration.ofSeconds(1), 1, null, null)

            // We keep track of the Events triggered:
            List<ChainForkEvent> forkEvents = new ArrayList<>()
            List<ChainPruneEvent> pruneEvents = new ArrayList<>()

            db.EVENTS().FORKS.forEach({ e ->
                forkEvents.add(e)
                println(" > EVENT DETECTED : Fork : parentBlock: " + e.parentForkHash.toString())
            })
            db.EVENTS().PRUNINGS.forEach({e ->
                pruneEvents.add(e)
                println(" > EVENT DETECTED : Prune : parentBlock: " + e.parentForkHash + " , num Blocks pruned: " + e.numBlocksPruned)
            })

        when:
            db.start()

            // We insert the main Chain: [genesis] -> 1 -> 2 -> 3 -> 4

            BlockHeader block_1 = TestingUtils.buildBlock(genesisBlock.getHash().toString())
            BlockHeader block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            BlockHeader block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            BlockHeader block_4 = TestingUtils.buildBlock(block_3.getHash().toString())

            println(" - Saving a Chain with 4 Blocks:")
            println("  Genesis:" + genesisBlock.getHash().toString())
            println("  Block 1:  |- " + block_1.getHash().toString())
            println("  Block 2:       |- " + block_2.getHash().toString())
            println("  Block 3:            |- " + block_3.getHash().toString())
            println("  Block 4:                 |- " + block_4.getHash().toString())
            db.saveBlocks(Arrays.asList(block_1, block_2, block_3, block_4))

            // We insert several FORKS:
            // [genesis] -> 1 -> 2  -> 3  -> 4
            //                     \-> 3B
            //               \-> 2C -> 3C

            BlockHeader block_3B = TestingUtils.buildBlock(block_2.getHash().toString())
            println(" - Saving a Block 3B that will create a FORK:")
            println("  Genesis:" + genesisBlock.getHash().toString())
            println("  Block 1:  |- " + block_1.getHash().toString())
            println("  Block 2:       |- " + block_2.getHash().toString())
            println("  Block 3:            |- " + block_3.getHash().toString())
            println("  Block 4:                 |- " + block_4.getHash().toString())
            println("  Block 3B:           |- " + block_3B.getHash().toString())
            db.saveBlock(block_3B)
            Thread.sleep(500)

            BlockHeader block_2C = TestingUtils.buildBlock(block_1.getHash().toString())
            BlockHeader block_3C = TestingUtils.buildBlock(block_2C.getHash().toString())
            println(" - Saving 2 Blocks: 2C and 3C that will create another FORK:")
            println("  Block 1:  |- " + block_1.getHash().toString())
            println("  Block 2:       |- " + block_2.getHash().toString())
            println("  Block 3:            |- " + block_3.getHash().toString())
            println("  Block 4:                 |- " + block_4.getHash().toString())
            println("  Block 3B:           |- " + block_3B.getHash().toString())
            println("  Block 2C:      |- " + block_3B.getHash().toString())
            println("  Block 3C:           |- " + block_3B.getHash().toString())

            db.saveBlock(block_2C)
            db.saveBlock(block_3C)

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

            // We check the DB Content in the console...
            db.printKeys()

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
            println(" - Cleanup...")
            db.removeBlocks(Arrays.asList(genesisBlock.getHash(),  block_1.getHash(), block_2.getHash(),
                    block_3.getHash(), block_4.getHash(), block_3B.getHash(), block_3C.getHash(), block_2C.getHash()))
            db.removeTipsChains()
            // We check the DB Content in the console...
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }
}
