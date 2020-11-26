package com.nchain.jcl.store.levelDB.blockStore

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.levelDB.common.TestingUtils
import spock.lang.Specification

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class with scenarios specific for Blocks
 */
class BlockStoreBlocksSpec extends Specification {

    /**
     * We test that the number of Blocks returned by the interface is correct
     */
    def "testing counting Blocks"() {
        final int NUM_BLOCKS = 5
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .workingFolder(dbPath)
                    .build()
            BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerBlockEvents(true)
                    .build()
        when:
            db.start()
            // We save several Blocks:
            for (int i = 0; i < NUM_BLOCKS; i++) db.saveBlock(TestingUtils.buildBlock())
            int numBlocks = db.getNumBlocks()
            db.stop()
        then:
            numBlocks == NUM_BLOCKS
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * We test that Blocks are saved and removed properly into the DB, and the related Events are also triggered
     */
    def "testing saving/removing Blocks"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .workingFolder(dbPath)
                    .build()
            BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerBlockEvents(true)
                    .build()

            // for keeping track of the Events triggered:
            AtomicInteger numBlockStoredEvents = new AtomicInteger()
            AtomicInteger numBlocksRemovedEvents = new AtomicInteger()

            db.EVENTS().BLOCKS_SAVED.forEach({ e -> numBlockStoredEvents.incrementAndGet()})
            db.EVENTS().BLOCKS_REMOVED.forEach({e -> numBlocksRemovedEvents.incrementAndGet()})

        when:
            // We save one BlockHeader, and then we retrieve it:
            BlockHeader blockHeader = TestingUtils.buildBlock()
            db.saveBlock(blockHeader)
            BlockHeader blockInserted = db.getBlock(blockHeader.getHash()).get()

            // We check that the Block retrieved is actually the one we inserted:
            boolean found = blockInserted != null && blockInserted.equals(blockHeader)

            // Now we remove it and we check that it's been actually removed:
            db.removeBlock(blockHeader.getHash())
            boolean removedAfter = db.getBlock(blockHeader.getHash()).isEmpty()

            // We wait a bit, to give the events enough time to reach their callbacks:
            Thread.sleep(100)

        then:
            found
            removedAfter
            numBlockStoredEvents.get() == 1
            numBlocksRemovedEvents.get() == 1
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }
}
