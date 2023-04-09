package io.bitcoinsv.jcl.store.blockStore

import io.bitcoinsv.jcl.tools.common.TestingUtils
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.jcl.store.blockStore.metadata.provided.BlockValidationMD
import io.bitcoinsv.jcl.store.blockStore.metadata.provided.TxValidationMD

/**
 * Testing class for the Metadata that can be linked to one specific Tx (Save, remove, etc)
 */
abstract class BlockStoreTxMetadataSpecBase extends BlockStoreSpecBase {

    /**
     * We test that we can save metadata linked to a Tx, and retrieve it later on.
     * We save several Txs, assigning different Metadata to each of them, and check that we retrieve the information
     * right after saving.
     */
    def "testing saving Txs and Metadata"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", true, false, BlockValidationMD.class, TxValidationMD.class)
        when:
            db.start()
            // We define 2 Blocks:
            Tx tx1 = TestingUtils.buildTx()
            Tx tx2 = TestingUtils.buildTx()

            // And 2 metadata, one per each:
            TxValidationMD meta1 = new TxValidationMD(true)
            TxValidationMD meta2 = new TxValidationMD(false)

            // We save The blocks, and then the metadata
            db.saveTx(tx1)
            db.saveTxMetadata(tx1.getHash(), meta1)
            db.saveTx(tx2)
            db.saveTxMetadata(tx2.getHash(), meta2)

            // We check the DB Content in the console...
            db.printKeys()

            // Now we retrieve the values...

            TxValidationMD meta1Read = (TxValidationMD) db.getTxMetadata(tx1.getHash()).get()
            TxValidationMD meta2Read = (TxValidationMD) db.getTxMetadata(tx2.getHash()).get()

        then:
            meta1 == meta1Read
            meta2 == meta2Read
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
            BlockStore db = getInstance("BSV-Main", true, false, BlockValidationMD.class, TxValidationMD.class)
        when:
            db.start()
            // We define 2 Blocks:
            Tx tx1 = TestingUtils.buildTx()
            Tx tx2 = TestingUtils.buildTx()

            // And 2 metadata, one per each:
            TxValidationMD meta1 = new TxValidationMD(true)
            TxValidationMD meta2 = new TxValidationMD(false)

            // We save The blocks, and then the metadata
            db.saveTx(tx1)
            db.saveTxMetadata(tx1.getHash(), meta1)
            db.saveTx(tx2)
            db.saveTxMetadata(tx2.getHash(), meta2)

            // Now we remove the Blocks
            db.removeTxs(Arrays.asList(tx1.getHash(), tx2.getHash()))

            // We check the DB Content in the console...
            db.printKeys()

            // We check that the metadata is also removed...
            Optional<TxValidationMD> meta1Read = db.getTxMetadata(tx1.getHash())
            Optional<TxValidationMD> meta2Read = db.getTxMetadata(tx2.getHash())

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
