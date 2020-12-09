package com.nchain.jcl.store.foundationDB.blockStore

import com.apple.foundationdb.Database
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.store.foundationDB.common.TestingUtils
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class with scenarios specific for Blocks
 */
class BlockStoreBlocksSpec extends Specification {

    /**
     * We test that Blocks are saved and removed properly into the DB, and the related Events are also triggered
     */
    def "testing saving/removing Blocks"() {
        given:
            println(" - Connecting to the DB...")
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .triggerBlockEvents(true)
                    .build()

            // We keep track of the Events triggered:
            AtomicInteger numBlocksSavedEvents = new AtomicInteger()
            AtomicInteger numBlocksRemovedEvents = new AtomicInteger();
            blockStore.EVENTS().BLOCKS_SAVED.forEach({e -> numBlocksSavedEvents.incrementAndGet()})
            blockStore.EVENTS().BLOCKS_REMOVED.forEach({e -> numBlocksRemovedEvents.incrementAndGet()})

        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)
            // We define 3 Blocks:
            BlockHeader block1 = TestingUtils.buildBlock()
            BlockHeader block2 = TestingUtils.buildBlock()
            BlockHeader block3 = TestingUtils.buildBlock()

            // We "simulate" the blocks 2 has 5 Txs...
            block2 = block2.toBuilder().numTxs(5).build()

            // We save 1 individual block:
            blockStore.saveBlock(block1)

            // We save a Batch with 2 Blocks:
            boolean existsBlock2BeforeInserted = blockStore.containsBlock(block2.getHash())
            blockStore.saveBlocks(Arrays.asList(block2, block3))
            boolean existsBlock2AfterInserted = blockStore.containsBlock(block2.getHash())

            // We check the number of Blocks inserted, and also the content of one of them:
            long numBlocksInserted = blockStore.getNumBlocks()
            BlockHeader block2Read = blockStore.getBlock(block2.getHash()).get()

            blockStore.printKeys()

            // We remove one individual Blocks and check the number of Blocks remaining:
            blockStore.removeBlock(block1.getHash())
            long numBlocksAfter1Remove = blockStore.getNumBlocks()

            // We remove a Batch of 2 Blocks and check the number of Blocks remaining:
            blockStore.removeBlocks(Arrays.asList(block2.getHash(), block3.getHash()))
            long numBlocksAfter2Remove = blockStore.getNumBlocks()

            // We wait a litle bit, so the Events have time to reach their callbacks:
            Thread.sleep(100)

            println(" Done.")
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
            blockStore.printKeys()
            blockStore.stop()
    }

}
