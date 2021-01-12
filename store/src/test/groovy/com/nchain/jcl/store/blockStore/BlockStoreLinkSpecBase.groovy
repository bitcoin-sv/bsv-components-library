package com.nchain.jcl.store.blockStore


import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.common.TestingUtils

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
abstract class BlockStoreLinkSpecBase extends BlockStoreSpecBase {

    /**
     * We test that the TxsRemovedEvent are triggered in a paginated way when we remove all the TXs within a Block.
     */
    def "testing Save/RemoveBlockTxs and Events Triggered"() {

        final int NUM_TXS =3

        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, true)

            // for keeping track of the Events triggered:
            AtomicInteger numTxsStoredEvents = new AtomicInteger()
            AtomicInteger numTxsRemovedEvents = new AtomicInteger()

            AtomicReference<List<String>> txsSaved = new AtomicReference<>(new ArrayList<>())
            AtomicReference<List<String>> txsRemoved = new AtomicReference<>(new ArrayList<>())

            db.EVENTS().TXS_SAVED.forEach({ e ->
                numTxsStoredEvents.incrementAndGet()
                txsSaved.get().addAll(e.txHashes.stream().map({h -> h.toString()}).collect(Collectors.toList()))
                //println("> TxsSavedEvent :: " + e.txHashes.size() + " Txs received.")
            })
            db.EVENTS().TXS_REMOVED.forEach({e ->
                numTxsRemovedEvents.incrementAndGet()
                txsRemoved.get().addAll(e.txHashes.stream().map({h -> h.toString()}).collect(Collectors.toList()))
                //println("> TxsRemovedEvent :: " + e.txHashes.size() + " Txs received.")
            })

        when:
            db.start()

            // We save a Block...
            BlockHeader blockHeader = TestingUtils.buildBlock()
            println(" - Saving Block " + blockHeader.getHash().toString() + "...")
            db.saveBlock(blockHeader)

            // We saved some TXs linked to that Block
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) {
                txs.add(TestingUtils.buildTx())
            }

            // We save and link these Txs to the previous block:

            println(" - Saving and linking " + NUM_TXS + " txs to Block " + blockHeader.getHash().toString() + "...")
            txs.forEach({tx -> println(" - Tx " + tx.getHash().toString())})
            db.saveBlockTxs(blockHeader.getHash(), txs)

            // We check the DB Content in the console...
            db.printKeys()

            // Now we remove the TXs just saved for this block.
            println(" - Removing all Txs linked to Block " + blockHeader.getHash().toString() + "...")
            db.removeBlockTxs(blockHeader.getHash())

            // We check the DB Content in the console...
            db.printKeys()

            // We check that all the Txs published are right and nothing is missing:
            boolean txsSavedOK = txsSaved.get().stream().allMatch({h -> txs.stream().anyMatch({tx -> tx.getHash().toString().equals(h)})})
            boolean txsRemovedOK = txsRemoved.get().stream().allMatch({h -> txs.stream().anyMatch({tx -> tx.getHash().toString().equals(h)})})

            // We give it some time for the event callbacks to be triggered:
            Thread.sleep(100)
        then:
            numTxsStoredEvents.get() ==   1
            numTxsRemovedEvents.get() >  0
            txsSaved.get().size() == NUM_TXS
            txsRemoved.get().size() == NUM_TXS
            txsSavedOK
            txsRemovedOK
        cleanup:
            println(" - Cleanup:")
            db.removeBlock(blockHeader.getHash())
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test that a Tx can be linked to more than 1 Block
     */
    def "Testing linking Tx to several Blocks"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", true, false)
        when:
            db.start()
            //TestingUtils.clearDB(blockStore.db)

            // We create several blocks and save them;
            BlockHeader block1 = TestingUtils.buildBlock()
            BlockHeader block2 = TestingUtils.buildBlock()
            println(" - Saving a Batch with 2 Blocks:")
            println(" - block 1: " + block1.getHash().toString())
            println(" - block 2: " + block2.getHash().toString())
            db.saveBlocks(Arrays.asList(block1, block2))

            // We create 4 Txs and link them so, both Blocks have one Tx in common:
            Tx tx1 = TestingUtils.buildTx()
            Tx tx2 = TestingUtils.buildTx()
            Tx tx3 = TestingUtils.buildTx()
            Tx tx4 = TestingUtils.buildTx()
            println(" - 4 Tx created (not saved yet):")
            println(" - tx 1: " + tx1.getHash().toString())
            println(" - tx 2: " + tx2.getHash().toString())
            println(" - tx 3: " + tx3.getHash().toString())
            println(" - tx 4: " + tx4.getHash().toString())

            println(" - Tx (Shared by 2 Blocks): " + tx2.getHash().toString())

            // Both Blocks have Tx2 in common:
            println(" - Linking Block " + block1.getHash().toString() + " to 2 txs:")
            println(" - tx 1: " + tx1.getHash().toString())
            println(" - tx 2: " + tx2.getHash().toString() + " (shared Tx)")
            db.saveBlockTxs(block1.getHash(), Arrays.asList(tx1, tx2))

            println(" - Linking Block " + block2.getHash().toString() + " to 3 txs:")
            println(" - tx 2: " + tx2.getHash().toString() + " (shared Tx)")
            println(" - tx 3: " + tx3.getHash().toString())
            println(" - tx 4: " + tx4.getHash().toString())
            db.saveBlockTxs(block2.getHash(), Arrays.asList(tx2, tx3, tx4))

            // We check the DB Content in the console...
            db.printKeys()

            List<Sha256Wrapper> blocksFromTx1 = db.getBlockHashLinkedToTx(tx1.getHash())
            List<Sha256Wrapper> blocksFromTx2 = db.getBlockHashLinkedToTx(tx2.getHash())
            List<Sha256Wrapper> blocksFromTx3 = db.getBlockHashLinkedToTx(tx3.getHash())
            List<Sha256Wrapper> blocksFromTx4 = db.getBlockHashLinkedToTx(tx4.getHash())

            boolean OK_txShared =   db.isTxLinkToblock(tx2.getHash(), block1.getHash()) &&
                                    db.isTxLinkToblock(tx2.getHash(), block2.getHash())


            Thread.sleep(100)
            // We check the DB Content in the console...
            db.printKeys()
            Thread.sleep(100)
        then:
            blocksFromTx1.size() == 1
            blocksFromTx2.size() == 2
            blocksFromTx3.size() == 1
            blocksFromTx4.size() == 1

            OK_txShared
        cleanup:
            println(" - Cleanup:")
            db.removeTxs(Arrays.asList(tx1.getHash(), tx2.getHash(), tx3.getHash(), tx4.getHash()))
            db.removeBlocks(Arrays.asList(block1.getHash(), block2.getHash()))
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test the Iterable returned when we get all the Txs belonging to 1 block
     */
    def "testing BlockTXs Iterable"() {
        final int NUM_TXS = 10
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", true, false)
        when:
            db.start()

            // We create and save a Block
            BlockHeader block = TestingUtils.buildBlock()
            println(" - Saving Block " + block.getHash().toString() + "...")
            db.saveBlock(block)

            // Now we create a list of TXs and we link them to this block:
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txs.add(TestingUtils.buildTx())

            println(" - Saving " + NUM_TXS + " txs and linked them to Block " + block.getHash().toString() + "...")
            db.saveBlockTxs(block.getHash(), txs)
            txs.forEach({tx -> println(" - tx " + tx.getHash().toString() + " saved and linked.")})

            // Now we are going to extract them using an Iterable, and we make sure that we extract all of them, so
            // we use a Map to keep track of them;
            Map<Sha256Wrapper, Boolean> txsRead = txs
                .stream()
                .collect(Collectors.toMap({ tx -> tx.getHash()}, { h -> false}))

            // We check the DB Content in the console...
            db.printKeys()

            // Now we use the Iterable to loop over the Txs linked to that Block...
            println(" - Getting a Iterable over the Txs linked to the Block " + block.getHash().toString() + "...")
            Iterator<Sha256Wrapper> txsIt = db.getBlockTxs(block.getHash()).iterator()
            while (txsIt.hasNext()) {
                Sha256Wrapper key = txsIt.next();
                System.out.println(" - Reading Tx from Iterator : " + key.toString());
                txsRead.put(key, true)
            }

            boolean ok = txsRead.size() == txs.size() &&
                txsRead.values().stream().filter({v -> !v}).count() == 0
        then:
            ok
        cleanup:
            println(" - Cleanup:")
            db.removeBlockTxs(block.getHash())
            db.removeBlock(block.getHash())
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test that a Tx can be linked/unlinked from/to a Block and the underlying information in the Db keeps consistent
     */
    def "testing linking and unlinking Txs from/to a Block"() {
        final int NUM_TXS = 3
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", true, false)
        when:
            db.start()

            // We save 2 Blocks:
            BlockHeader block1 = TestingUtils.buildBlock()
            BlockHeader block2 = TestingUtils.buildBlock()
            println(" - Saving Block " + block1.getHash().toString() + "...")
            println(" - Saving Block " + block2.getHash().toString() + "...")
            db.saveBlock(block1)
            db.saveBlock(block2)

            // We create several Txs and save them
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) {
                txs.add(TestingUtils.buildTx())
            }
            println(" - Saving " + NUM_TXS + " Txs:")
            txs.forEach({tx -> println(" - tx " + tx.getHash().toString() + " saved.")})
            db.saveTxs(txs)

            // No we link all of them with the FIRST Block:
            println(" - Linking the " + NUM_TXS + " txs to Block " + block1.getHash().toString() + ":")
            txs.forEach({tx ->
                db.linkTxToBlock(tx.getHash(), block1.getHash())
                println(" - tx " + tx.getHash().toString() + " linked.")
            })

            // We check the DB Content in the console...
            db.printKeys()

            // We take the FIRST Tx and we also Link it with the SECOND Block:
            Tx sharedTx = txs.get(0)
            db.linkTxToBlock(sharedTx.getHash(), block2.getHash())
            println(" - Linking the Tx " + sharedTx.getHash().toString() + " to Block " + block2.getHash().toString() + "...")

            // We check the DB Content in the console...
            db.printKeys()

            // We check the Txs have been properly linked to the block 1:
            int numTxsLinkedBlock1 = db.getBlockNumTxs(block1.getHash())
            boolean txsLinkedBlock1_OK = true
            List<Sha256Wrapper> txsLinkedBlock1Hashes = new ArrayList()

            // We check that all the Txs linked to this Block1 are correct (they are also present in the original
            // List of Txs...
            Iterator<Sha256Wrapper> txsLinkedBlock1It = db.getBlockTxs(block1.getHash()).iterator()
            while (txsLinkedBlock1It.hasNext()) {
                Sha256Wrapper txHash = txsLinkedBlock1It.next()
                txsLinkedBlock1_OK &= !txsLinkedBlock1Hashes.contains(txHash) && txs.stream().anyMatch({tx -> tx.getHash().equals(txHash)})
                txsLinkedBlock1Hashes.add(txHash)
            }
            // And we also check that the number of Txs is correct
            boolean numTxsLinkedBlock1OK = (numTxsLinkedBlock1 == txs.size())

            // We check that the FIRST Tx has been linked to the FIRST and SECOND Blocks:
            boolean txsLinkedBlock2_OK = true
            int numTxLinkedBlock2 = db.getBlockNumTxs(block2.getHash())
            Iterator<Sha256Wrapper> txsLinkedBlock2It = db.getBlockTxs(block2.getHash()).iterator()
            Sha256Wrapper txLinkedBlock2Hash = txsLinkedBlock2It.next()

            txsLinkedBlock2_OK = txLinkedBlock2Hash.equals(sharedTx.getHash()) && (numTxLinkedBlock2 == 1)

            // Now we unlink the Shared TX from all its Blocks (block1 and Block 2) and check the result:
            println(" - Unlinking Tx " + sharedTx.getHash().toString() + "...")
            db.unlinkTx(sharedTx.getHash())

            int numTxsLinkedToBlock1AfterUnlinkingSharedTx = db.getBlockNumTxs(block1.getHash())
            int numTxsLinkedToBlock2AfterUnlinkingSharedTx = db.getBlockNumTxs(block2.getHash())
            int numBlocksLinkedtoSharedTxAfterUnlinkingSharedTx = db.getBlockHashLinkedToTx(sharedTx.getHash()).size()

            // Now we unlink the Block1 from all its Txs an we check the result:
            println(" - Unlinking Block " + block1.getHash().toString() + "...")
            db.unlinkBlock(block1.getHash())

            println(" - Checking that all the Txs has NO blocks linked to them:")
            boolean block1Unlinked_OK = true;
            for (int i = 1; i < txs.size(); i++) {
                List<Sha256Wrapper> blocksLinked = db.getBlockHashLinkedToTx(txs.get(i).getHash())
                println(" - tx " + txs.get(i).getHash().toString() + " , " + blocksLinked.size() + " blocks linked to it")
                block1Unlinked_OK &= (blocksLinked.size() == 0)
            }

            // We check the DB Content in the console...
            db.printKeys()

        then:
            txsLinkedBlock1_OK
            numTxsLinkedBlock1OK
            txsLinkedBlock2_OK
            numTxsLinkedToBlock1AfterUnlinkingSharedTx == (NUM_TXS - 1)
            numTxsLinkedToBlock2AfterUnlinkingSharedTx == 0
            numBlocksLinkedtoSharedTxAfterUnlinkingSharedTx == 0
            block1Unlinked_OK
        cleanup:
            println(" - Cleanup:")
            db.removeTxs(txs.stream().map({tx -> tx.getHash()}).collect(Collectors.toList()))
            db.removeBlock(block1.getHash())
            db.removeBlock(block2.getHash())
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }
}
