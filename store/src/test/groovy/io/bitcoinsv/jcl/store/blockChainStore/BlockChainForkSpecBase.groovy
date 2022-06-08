package io.bitcoinsv.jcl.store.blockChainStore


import io.bitcoinsv.jcl.store.blockChainStore.events.ChainForkEvent
import io.bitcoinsv.jcl.store.blockChainStore.events.ChainPruneEvent
import io.bitcoinsv.jcl.tools.common.TestingUtils
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import spock.lang.Ignore

import java.time.Duration

/**
 * Test scenarios involving a Fork and Prune operations
 */
abstract class BlockChainForkSpecBase extends BlockChainStoreSpecBase {

    /**
     * We create a Main Chain, and then a Fork Chain. We check that the Fork event is triggered. Then we prune the
     * fork chain, and we check that the chain is pruned and the event triggered.
     */
    def "Testing Fork and Prunning"() {
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
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

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We insert the main Chain: [genesis] -> 1 -> 2 -> 3 -> 4

            HeaderReadOnly block_1 = TestingUtils.buildBlock(genesisBlock.getHash().toString())
            HeaderReadOnly block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            HeaderReadOnly block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            HeaderReadOnly block_4 = TestingUtils.buildBlock(block_3.getHash().toString())

            println(" - Inserting a Chain of 5 blocks:")
            db.saveBlocks(Arrays.asList(block_1, block_2, block_3, block_4))

            println("  Genesis:" + genesisBlock.getHash().toString())
            println("  Block 1:  |- " + block_1.getHash().toString())
            println("  Block 2:       |- " + block_2.getHash().toString())
            println("  Block 3:            |- " + block_3.getHash().toString())
            println("  Block 4:                 |- " + block_4.getHash().toString())

            // We verify the Tips fo the Chains:
            List<Sha256Hash> tips = db.getTipsChains()
            println(" - Chain Tips:")
            tips.forEach({ t -> println(" - Block " + t.toString())})

            // We insert a FORK Branch:
            // [genesis] -> 1 -> 2 -> 3  -> 4
            //                    \-> 3B -> 4B

            HeaderReadOnly block_3B = TestingUtils.buildBlock(block_2.getHash().toString())
            HeaderReadOnly block_4B = TestingUtils.buildBlock(block_3B.getHash().toString())
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

            boolean tipsOKBeforePruning = (tips.size() == 2) && (tips.contains(block_4.getHash())) && (tips.contains(block_4B.getHash()))

            // We also verify the tips of some specific blocks: 2 and 3
            List<Sha256Hash> tipsBlock2BeforePrunning = db.getTipsChains(block_2.hash)
            List<Sha256Hash> tipsBlock3BeforePrunning = db.getTipsChains(block_3.hash)

            // and now we prune it:
            println(" > Prunning from Tip: " + block_4B.getHash() + " ...")
            db.prune(block_4B.getHash(), false)

            // Now we check the fork chain is gone:
            tips = db.getTipsChains()
            println(" - Chain Tips:")
            tips.forEach({ t -> println(" - Block " + t.toString())})

            boolean tipsOKAfterPruning = (tips.size() == 1) && (tips.get(0).equals(block_4.getHash()))

            // We also verify the tips of some specific blocks: 2 and 3
            List<Sha256Hash> tipsBlock2AfterPrunning = db.getTipsChains(block_2.hash)
            List<Sha256Hash> tipsBlock3AfterPrunning = db.getTipsChains(block_3.hash)

            // Some waiting, to give the events time to reach the callbacks:
            Thread.sleep(50)

            // We check the DB Content in the console...
            db.printKeys()

        then:
            tipsOKBeforePruning
            tipsBlock2BeforePrunning.size() == 2
            tipsBlock2BeforePrunning.contains(block_4.hash)
            tipsBlock2BeforePrunning.contains(block_4B.hash)
            tipsBlock3BeforePrunning.size() == 1
            tipsBlock3BeforePrunning.contains(block_4.hash)

            pruneEvents.size() == 1
            pruneEvents.get(0).tipForkHash.equals(block_4B.getHash())
            pruneEvents.get(0).parentForkHash.equals(block_2.getHash())
            pruneEvents.get(0).numBlocksPruned == 2
            forkEvents.size() == 1
            forkEvents.get(0).blockForkHash.equals(block_3B.getHash())
            forkEvents.get(0).parentForkHash.equals(block_2.getHash())

            tipsOKAfterPruning
            tipsBlock2AfterPrunning.size() == 1
            tipsBlock2AfterPrunning.contains(block_4.hash)
            tipsBlock3AfterPrunning.size() == 1
            tipsBlock3AfterPrunning.contains(block_4.hash)

        cleanup:
            println(" - Cleanup...")
            db.removeBlocks(Arrays.asList(genesisBlock.getHash(), block_1.getHash(), block_2.getHash(),
                    block_3.getHash(), block_4.getHash(), block_3B.getHash(), block_4B.getHash()))
            db.removeTipsChains()
            db.clear()
            // We check the DB Content in the console...
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test that prunning a LONG Chain works fine and there are no StackTrace issues...
     */
    @Ignore // time Consuming
    def "Testing Prunning a Long Chain"() {
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, null, null)

        when:
            db.start()

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We insert the main Chain: [genesis] -> ...100 blocks
            Sha256Hash parentBlockHash = genesisBlock.hash
            for (int i = 0; i < 100; i++) {
                HeaderReadOnly block = TestingUtils.buildBlock(parentBlockHash.toString())
                db.saveBlock(block)
                parentBlockHash = block.hash
            }

            // Now we create 1 separate Chains: A and B: starting from the last block, These chains are quite long (100K)
            Sha256Hash parentBlockHashA = parentBlockHash;
            Sha256Hash parentBlockHashB = parentBlockHash;
            for (int i = 0; i < 10_000; i++) {
                HeaderReadOnly newBlockA = TestingUtils.buildBlock(parentBlockHashA.toString())
                HeaderReadOnly newBlockB = TestingUtils.buildBlock(parentBlockHashB.toString())
                db.saveBlock(newBlockA)
                db.saveBlock(newBlockB)
                parentBlockHashA = newBlockA.hash
                parentBlockHashB = newBlockB.hash
            }

            // We check the tips:
            List<Sha256Hash> tipsBeforePrunning = db.getTipsChains()

            // Now we prune one of them:
            db.prune(tipsBeforePrunning.get(0), true)

            // And now we check the tips again:
            List<Sha256Hash> tipsAfterPrunning = db.getTipsChains()

            // And we prune the remainnig Tip. This would prune the whole DB:
            db.prune(tipsAfterPrunning.get(0), true)
            long numBlocksEnd = db.getNumBlocks()
            Thread.sleep(100)

        then:
            tipsBeforePrunning.size() == 2
            tipsBeforePrunning.contains(parentBlockHashA)
            tipsBeforePrunning.contains(parentBlockHashB)

            tipsAfterPrunning.size() == 1
            numBlocksEnd == 1 // GENESIS

        cleanup:
            println(" - Cleanup...")
            db.clear()
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
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
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

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We insert the main Chain: [genesis] -> 1 -> 2 -> 3 -> 4

            HeaderReadOnly block_1 = TestingUtils.buildBlock(genesisBlock.getHash().toString())
            HeaderReadOnly block_2 = TestingUtils.buildBlock(block_1.getHash().toString())
            HeaderReadOnly block_3 = TestingUtils.buildBlock(block_2.getHash().toString())
            HeaderReadOnly block_4 = TestingUtils.buildBlock(block_3.getHash().toString())

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

            HeaderReadOnly block_3B = TestingUtils.buildBlock(block_2.getHash().toString())
            println(" - Saving a Block 3B that will create a FORK:")
            println("  Genesis:" + genesisBlock.getHash().toString())
            println("  Block 1:  |- " + block_1.getHash().toString())
            println("  Block 2:       |- " + block_2.getHash().toString())
            println("  Block 3:            |- " + block_3.getHash().toString())
            println("  Block 4:                 |- " + block_4.getHash().toString())
            println("  Block 3B:           |- " + block_3B.getHash().toString())
            db.saveBlock(block_3B)
            Thread.sleep(500)

            HeaderReadOnly block_2C = TestingUtils.buildBlock(block_1.getHash().toString())
            HeaderReadOnly block_3C = TestingUtils.buildBlock(block_2C.getHash().toString())
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
            List<Sha256Hash> tips = db.getTipsChains()
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
            db.clear()
            db.stop()
            println(" - Test Done.")
    }
}
