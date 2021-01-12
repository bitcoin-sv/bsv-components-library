package com.nchain.jcl.store.blockStore

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.store.common.TestingUtils


import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class for Basic Scenarios with Blocks (inserting, removing, etc).
 *
 * This class can NOT be tested itself, it needs to be extended. An extending class must implement the "getInstance"
 * method, which returns a concrete implementation of the BlockStore interface (like a LevelDB or FoundationDB
 * Implementation).
 *
 * Once that method is implemented, the extending class can be tested without any other additions, since running the
 * extending class will automatically trigger the tests defined in THIS class.
 */
abstract class BlockStoreBlocksSpecBase extends BlockStoreSpecBase {

    /**
     * We test that Blocks are saved and removed properly into the DB, and the related Events are also triggered
     */
    def "testing saving/removing Blocks"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", true, false)

            // We keep track of the Events triggered:
            AtomicInteger numBlocksSavedEvents = new AtomicInteger()
            AtomicInteger numBlocksRemovedEvents = new AtomicInteger();
            db.EVENTS().BLOCKS_SAVED.forEach({e -> numBlocksSavedEvents.incrementAndGet()})
            db.EVENTS().BLOCKS_REMOVED.forEach({e -> numBlocksRemovedEvents.incrementAndGet()})

        when:
            db.start()
            // We define 3 Blocks:
            BlockHeader block1 = TestingUtils.buildBlock()
            BlockHeader block2 = TestingUtils.buildBlock()
            BlockHeader block3 = TestingUtils.buildBlock()

            // We "simulate" the blocks 2 has 5 Txs...
            block2 = block2.toBuilder().numTxs(5).build()

            // We save 1 individual block:
            println(" - Saving Block " + block1.getHash().toString() + "...")
            db.saveBlock(block1)

            // We save a Batch with 2 Blocks:
            println(" - Saving a Batch with 2 Blocks: ")
            println(" - Block " + block2.getHash().toString() + "...")
            println(" - Block " + block3.getHash().toString() + "...")
            boolean existsBlock2BeforeInserted = db.containsBlock(block2.getHash())
            db.saveBlocks(Arrays.asList(block2, block3))
            boolean existsBlock2AfterInserted = db.containsBlock(block2.getHash())

            // We check the number of Blocks inserted, and also the content of one of them:
            long numBlocksInserted = db.getNumBlocks()
            BlockHeader block2Read = db.getBlock(block2.getHash()).get()

            // We check the DB Content in the console...
            db.printKeys()

            // We remove one individual Block and check the number of Blocks remaining:
            println(" - Removing Block " + block1.getHash().toString() + "...")
            db.removeBlock(block1.getHash())
            long numBlocksAfter1Remove = db.getNumBlocks()

            // We remove a Batch of 2 Blocks and check the number of Blocks remaining:
            println(" - Removing a Batch with 2 Blocks: ")
            println(" - Block " + block2.getHash().toString() + "...")
            println(" - Block " + block3.getHash().toString() + "...")
            db.removeBlocks(Arrays.asList(block2.getHash(), block3.getHash()))
            long numBlocksAfter2Remove = db.getNumBlocks()

            // We check the DB Content in the console...
            db.printKeys()

            // We wait a little bit, so the Events have time to reach their callbacks:
            Thread.sleep(100)
        then:
            numBlocksInserted == 3
            numBlocksAfter1Remove == 2
            numBlocksAfter2Remove == 0
            block2Read.equals(block2)
            numBlocksSavedEvents.get() == 2
            numBlocksRemovedEvents.get() == 2
            !existsBlock2BeforeInserted
            existsBlock2AfterInserted

        cleanup:
            println(" - Cleanup...")
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }
}
