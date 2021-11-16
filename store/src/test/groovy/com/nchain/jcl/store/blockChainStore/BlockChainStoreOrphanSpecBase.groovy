package com.nchain.jcl.store.blockChainStore


import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStoreSpecBase
import com.nchain.jcl.store.common.TestingUtils
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import io.bitcoinj.core.Sha256Hash

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

/**
 * Testing class for Basic Scenarios with Orphan Blocks
 *
 * This class can NOT be tested itself, it needs to be extended. An extending class must implement the "getInstance"
 * method, which returns a concrete implementation of the BlockStore interface (like a LevelDB or FoundationDB
 * Implementation).
 *
 * Once that method is implemented, the extending class can be tested without any other additions, since running the
 * extending class will automatically trigger the tests defined in THIS class.
 */
abstract class BlockChainStoreOrphanSpecBase extends BlockChainStoreSpecBase {

    /**
     * We test that the "getOrphansBlocks()" works fine and we can iterate over the result.
     */
    def "testing iterating over Orphan Blocks"() {
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

            // We define a chain of Blocks:
            // [GENESIS] - [Block1] - [Block2]

            HeaderReadOnly block1 = TestingUtils.buildBlock(genesisBlock.hash.toString())
            HeaderReadOnly block2 = TestingUtils.buildBlock(block1.hash.toString())
            db.saveBlocks(Arrays.asList(block1, block2))

            // And now we define 2 more branches: These first branch can be connected to the main chain, but for now
            // we are NOT going to save the Blocks "[Block3]" and "[Block6]", so well end up with 3 branches:
            //  - the previous one starting from GENESIS
            //  - (Block3-MISSING) - [Block4] - [Block5]
            //  - [Block5] - (Block6-MISSING) - [Block7]

            HeaderReadOnly block3 = TestingUtils.buildBlock(block2.hash.toString())
            HeaderReadOnly block4 = TestingUtils.buildBlock(block3.hash.toString())
            HeaderReadOnly block5 = TestingUtils.buildBlock(block4.hash.toString())
            HeaderReadOnly block6 = TestingUtils.buildBlock(block5.hash.toString())
            HeaderReadOnly block7 = TestingUtils.buildBlock(block6.hash.toString())

            db.saveBlocks(Arrays.asList(block4, block5, block7))

            // We check the DB Content in the console...
            db.printKeys()

            // We get an iterator with the Orphans blocks a this moment, which should be [Block4] and [Block7]:
            println(" - Getting Orphans:")
            Set<Sha256Hash> orphanBlocks = new HashSet<>()
            Iterator<Sha256Hash> orphanBlocksIt = db.getOrphanBlocks().iterator()
            while (orphanBlocksIt.hasNext()) {
                Sha256Hash orphan = orphanBlocksIt.next()
                println(" Orphan block found: " + orphan)
                orphanBlocks.add(orphan)
            }

            // Now we connect everything, by inserting the missing blocks [block3] and [block4]. Now there shouldn't be
            // any orphan blocks at all..
            db.saveBlocks(Arrays.asList(block3, block6))

            // We check the DB Content in the console...
            db.printKeys()

            println(" - Getting Orphans AFTER Reconnecting the different Branches:")
            Set<Sha256Hash> orphanBlocksAfterConnecting = new HashSet<>()
            Iterator<Sha256Hash> orphanBlocksAfterConnectingIt = db.getOrphanBlocks().iterator()
            while (orphanBlocksAfterConnectingIt.hasNext()) {
                Sha256Hash orphan = orphanBlocksAfterConnectingIt.next()
                println(" Orphan block found: " + orphan)
                orphanBlocksAfterConnecting.add(orphan)
            }

        then:
            orphanBlocks.size() == 3
            orphanBlocks.contains(block4.hash)
            orphanBlocks.contains(block5.hash)
            orphanBlocks.contains(block7.hash)

            orphanBlocksAfterConnecting.size() == 0

        cleanup:
            db.removeBlocks(Arrays.asList(genesisBlock, block1, block2, block3, block4, block5, block6, block7)
                .stream()
                .map({b -> b.hash})
                .collect(Collectors.toList()))
            println(" - Cleanup...")
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test that the Automatic Orphan Prunning
     */
    def "testing Automatic Orphan Prunning"() {
        final Duration ORPHAN_PRUNNING_FREQUENCY = Duration.ofSeconds(1)
        final Duration ORPHAN_PRUNNING_AGE = Duration.ofSeconds(2)
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null, ORPHAN_PRUNNING_FREQUENCY, ORPHAN_PRUNNING_AGE)

        when:
            db.start()

            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            // We define a chain of Blocks, and we addBytes Orphans at the end, with a delay between them, so the Automatic
            // Orphan pruning process removes them at different times:

            // We save a chain of 3 Blocks:
            // [GENESIS] - [Block1] - [Block2]
            HeaderReadOnly block1 = TestingUtils.buildBlock(genesisBlock.hash.toString())
            HeaderReadOnly block2 = TestingUtils.buildBlock(block1.hash.toString())

            println("Storing main chain of blocks:")
            println(" - Genesis: " + genesisBlock.hash)
            println("     |- Block 1: " + block1.hash)
            println("          |- Block 2: " + block2.hash)

            db.saveBlocks(Arrays.asList(block1, block2)) // Block 5 is ORPHAN

            // Now we save an Orphan:
            //  [*] - [Block 4]

            HeaderReadOnly block3 = TestingUtils.buildBlock(block2.hash.toString()) // Not SAVED
            HeaderReadOnly block4 = TestingUtils.buildBlock(block3.hash.toString())

            println("Storing an Orphan:")
            println("              |- * (missing)")
            println("                      |- Block 4: " + block4.hash)

            db.saveBlock(block4) // Block 4 is ORPHAN

            // We wait and we store Block5, a child of Block 4:
            // [*] - [Block 4] - [Block 5]

            Thread.sleep((long)(ORPHAN_PRUNNING_AGE.toMillis() / 2))

            HeaderReadOnly block5 = TestingUtils.buildBlock(block4.hash.toString())
            println("Storing an Orphan:")
            println("                      |- Block 4: " + block4.hash)
            println("                            |- Block 5: " + block5.hash)
            db.saveBlock(block5)

            // We get the current Chain Tips and the List or Orphans:
            // At this moment, the Automatic Orphan Pruning should have NOT been triggered yet:
            List<Sha256Hash> tipsBeforePrunning = db.getTipsChains()
            List<Sha256Hash> orphansBeforePrunning = new ArrayList<>()

            db.printKeys()
            Iterator<Sha256Hash> orphanBlocksIt = db.getOrphanBlocks().iterator()
            while (orphanBlocksIt.hasNext()) {
                Sha256Hash orphanHash = orphanBlocksIt.next()
                println(" Orphan found: " + orphanHash)
                orphansBeforePrunning.add(orphanHash)
            }

            // Now we wait a bit, enough for the Automatic Pruning to the triggered.
            Thread.sleep((long)(ORPHAN_PRUNNING_AGE.toMillis() / 2))
            // At this point, the automatic pruning should have Removed the Block 4...

            // We get the current Chain Tips and the List or Orphans:
            println("Getting Info from the Chain after FIRST PRunning:")
            List<Sha256Hash> tipsAfterFirstPrunning = db.getTipsChains()
            List<Sha256Hash> orphansAfterFirstPrunning = new ArrayList<>()
            Iterator<Sha256Hash> orphanBlocksIt2 = db.getOrphanBlocks().iterator()
            while (orphanBlocksIt2.hasNext()) {
                Sha256Hash orphanHash = orphanBlocksIt2.next()
                println("  - Orphan Block found: " + orphanHash)
                orphansAfterFirstPrunning.add(orphanHash)
            }

            // We wait a bit more, so the Automatic pruning is triggered again, this time the block 5 will also be
            // ellegible for Pruning:
            Thread.sleep(ORPHAN_PRUNNING_AGE.toMillis())

            // We get the current Chain Tips and the List or Orphans:
            println("Getting Info from he Chain after SECOND PRunning:")
            List<Sha256Hash> tipsAfterSecondPrunning = db.getTipsChains()
            List<Sha256Hash> orphansAfterSecondPrunning = new ArrayList<>()
            Iterator<Sha256Hash> orphanBlocksIt3 = db.getOrphanBlocks().iterator()
            while (orphanBlocksIt3.hasNext()) {
                Sha256Hash orphanHash = orphanBlocksIt3.next()
                println("  - Orphan Block found: " + orphanHash)
                orphansAfterFirstPrunning.add(orphanHash)
            }

            // We check the DB Content in the console...
            db.printKeys()

        then:
            tipsBeforePrunning.size() == 1
            tipsBeforePrunning.contains(block2.hash)

            orphansBeforePrunning.size() == 2
            orphansBeforePrunning.contains(block4.hash)

            tipsAfterFirstPrunning.size() == 1
            tipsAfterFirstPrunning.contains(block2.hash)
            orphansAfterFirstPrunning.size() == 1
            orphansAfterFirstPrunning.contains(block5.hash)

            tipsAfterSecondPrunning.size() == 1
            tipsAfterSecondPrunning.contains(block2.hash)
            orphansAfterSecondPrunning.size() == 0

        cleanup:
            db.removeBlocks(Arrays.asList(genesisBlock, block1, block2, block4, block5)
                    .stream()
                    .map({b -> b.hash})
                    .collect(Collectors.toList()))
            println(" - Cleanup...")
            db.printKeys()
            db.clear()
            db.stop()
            println(" - Cleanup...")
    }


}
