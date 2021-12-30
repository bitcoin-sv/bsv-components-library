package io.bitcoinsv.jcl.store.blockStore


import io.bitcoinsv.jcl.store.common.TestingUtils
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.jcl.store.blockStore.metadata.provided.BlockValidationMD

/**
 * Testing class for the Metadata that can be linked to one specific Block (Save, remove, etc)
 */
abstract class BlockStoreBlockMetadataSpecBase extends BlockStoreSpecBase {

    /**
     * We test that we can save metadata linked to a Block, and retrieve it later on.
     * We save several Blocks, assigning different Metadata to each of them, and heck that we retrieve the information
     * right after saving.
     */
    def "testing saving Blocks and Metadata"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", true, false, BlockValidationMD.class)
        when:
            db.start()
            // We define 2 Blocks:
        HeaderReadOnly block1 = TestingUtils.buildBlock()
            HeaderReadOnly block2 = TestingUtils.buildBlock()

            // And 2 metadata, one per each:
            BlockValidationMD meta1 = new BlockValidationMD(5, false, false)
            BlockValidationMD meta2 = new BlockValidationMD(6, true, true)

            // We save The blocks, and then the metadata
            db.saveBlock(block1)
            db.saveBlockMetadata(block1.getHash(), meta1)
            db.saveBlock(block2)
            db.saveBlockMetadata(block2.getHash(), meta2)

            // We check the DB Content in the console...
            db.printKeys()

            // Now we retrieve the values...

            BlockValidationMD meta1Read = (BlockValidationMD) db.getBlockMetadata(block1.getHash()).get()
            BlockValidationMD meta2Read = (BlockValidationMD) db.getBlockMetadata(block2.getHash()).get()

        then:
            meta1.equals(meta1Read)
            meta2.equals(meta2Read)
        cleanup:
            println(" - Cleanup...")
            db.clear()
            Thread.sleep(2000)
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test that the metadata attached to a Block is removed when the Block is also removed
     */
    def "testing removing Blocks and Metadata"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", true, false, BlockValidationMD.class)
        when:
            db.start()
            // We define 2 Blocks:
            HeaderReadOnly block1 = TestingUtils.buildBlock()
            HeaderReadOnly block2 = TestingUtils.buildBlock()

            // And 2 metadata, one per each:
            BlockValidationMD meta1 = new BlockValidationMD(5, false, false)
            BlockValidationMD meta2 = new BlockValidationMD(6, true, true)

            // We save The blocks, and then the metadata
            db.saveBlock(block1)
            db.saveBlockMetadata(block1.getHash(), meta1)
            db.saveBlock(block2)
            db.saveBlockMetadata(block2.getHash(), meta2)

            // Now we remove the Blocks
            db.removeBlocks(Arrays.asList(block1.getHash(), block2.getHash()))

            // We check the DB Content in the console...
            db.printKeys()

            // We check that the metadata is also removed...
            Optional<BlockValidationMD> meta1Read = db.getBlockMetadata(block1.getHash())
            Optional<BlockValidationMD> meta2Read = db.getBlockMetadata(block2.getHash())

        then:
            meta1Read.isEmpty()
            meta2Read.isEmpty()
        cleanup:
            println(" - Cleanup...")
            db.clear()
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }
}
