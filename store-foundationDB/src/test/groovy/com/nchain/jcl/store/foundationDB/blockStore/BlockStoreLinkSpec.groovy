package com.nchain.jcl.store.foundationDB.blockStore

import com.apple.foundationdb.Database
import com.apple.foundationdb.directory.DirectoryLayer
import com.apple.foundationdb.directory.DirectorySubspace
import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.foundationDB.common.TestingUtils
import spock.lang.Specification

import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
class BlockStoreLinkSpec extends Specification {

    /**
     * We test that the TxsRemovedEvent are triggered in a paginated way when we remove all the TCs within a Block.
     */
    def "testing Save/RemoveBlockTxs and Events Triggered"() {

        final int NUM_TXS = 20

        given:
            println(" - Connecting to the DB...")
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .triggerBlockEvents(true)
                    .triggerTxEvents(true)
                    .build()

            // for keeping track of the Events triggered:
            AtomicInteger numTxsStoredEvents = new AtomicInteger()
            AtomicInteger numTxsRemovedEvents = new AtomicInteger()

            AtomicReference<List<String>> txsSaved = new AtomicReference<>(new ArrayList<>())
            AtomicReference<List<String>> txsRemoved = new AtomicReference<>(new ArrayList<>())

            blockStore.EVENTS().TXS_SAVED.forEach({ e ->
                numTxsStoredEvents.incrementAndGet()
                txsSaved.get().addAll(e.txHashes.stream().map({h -> h.toString()}).collect(Collectors.toList()))
                //println("> TxsSavedEvent :: " + e.txHashes.size() + " Txs received.")
            })
            blockStore.EVENTS().TXS_REMOVED.forEach({e ->
                numTxsRemovedEvents.incrementAndGet()
                txsRemoved.get().addAll(e.txHashes.stream().map({h -> h.toString()}).collect(Collectors.toList()))
                //println("> TxsRemovedEvent :: " + e.txHashes.size() + " Txs received.")
            })

        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)
            // We create a Block and insert TXs into it
            BlockHeader blockHeader = TestingUtils.buildBlock()
            println("Storing Block...")
            blockStore.saveBlock(blockHeader)
            List<Tx> txs = new ArrayList<>()
            println("Storing Txs linked ot this block...")
            for (int i = 0; i < NUM_TXS; i++) {
                txs.add(TestingUtils.buildTx())
            }

            // We also measure the time it takes to insert all these TXs:
            Instant initTime = Instant.now()
            blockStore.saveBlockTxs(blockHeader.getHash(), txs)

            blockStore.printKeys()
            println(Duration.between(initTime, Instant.now()).toMillis() + " millsecs to insert and link " + NUM_TXS + " Txs")

            // Now we remove the TXs just saved for this block.
            // We also measure the time here
            initTime = Instant.now()
            blockStore.removeBlockTxs(blockHeader.getHash())
            println(Duration.between(initTime, Instant.now()).toMillis() + " millsecs to remove and unlink " + NUM_TXS + " Txs")

            // We check that all the Txs published are right and nothiing is missing:
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
            blockStore.removeBlock(blockHeader.getHash())
            blockStore.printKeys()
            blockStore.stop()
    }

    /**
     * We test that a Tx can be linked to more than 1 Block
     */
    def "Testing linking Tx to several Blocks"() {
        given:
            println(" - Connecting to the DB...")
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .triggerBlockEvents(true)
                    .build()
        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)

            // We create several blocks and save them;
            BlockHeader block1 = TestingUtils.buildBlock()
            BlockHeader block2 = TestingUtils.buildBlock()
            blockStore.saveBlocks(Arrays.asList(block1, block2))

            // We create 4 Txs and link them so, both Blocks have one Tx in common:
            Tx tx1 = TestingUtils.buildTx()
            Tx tx2 = TestingUtils.buildTx()
            Tx tx3 = TestingUtils.buildTx()
            Tx tx4 = TestingUtils.buildTx()

            println("Tx2 (Shared by 2 Blocks): " + tx2.getHash().toString())

            // Both Blocks have Tx2 in common:
            blockStore.saveBlockTxs(block1.getHash(), Arrays.asList(tx1, tx2))
            blockStore.saveBlockTxs(block2.getHash(), Arrays.asList(tx2, tx3, tx4))

            List<Sha256Wrapper> blocksFromTx1 = blockStore.getBlockHashLinkedToTx(tx1.getHash())
            List<Sha256Wrapper> blocksFromTx2 = blockStore.getBlockHashLinkedToTx(tx2.getHash())
            List<Sha256Wrapper> blocksFromTx3 = blockStore.getBlockHashLinkedToTx(tx3.getHash())
            List<Sha256Wrapper> blocksFromTx4 = blockStore.getBlockHashLinkedToTx(tx4.getHash())


            boolean OK_txShared =   blockStore.isTxLinkToblock(tx2.getHash(), block1.getHash()) &&
                                    blockStore.isTxLinkToblock(tx2.getHash(), block2.getHash())

            boolean OKafterSavingAndLinking = (
                    blocksFromTx1.size() == 1 &&
                            blocksFromTx2.size() == 2 &&
                            blocksFromTx3.size() == 1 &&
                            blocksFromTx4.size() == 1
            )
            Thread.sleep(100)
            blockStore.printKeys()
            Thread.sleep(100)
        then:
            OKafterSavingAndLinking
            OK_txShared
        cleanup:
            blockStore.removeTxs(Arrays.asList(tx1.getHash(), tx2.getHash(), tx3.getHash(), tx4.getHash()))
            blockStore.removeBlocks(Arrays.asList(block1.getHash(), block2.getHash()))
            blockStore.printKeys()
            blockStore.stop()
    }

    /**
     * We test the Iterable returned when we get all the Txs belonging to 1 block
     */
    def "testing BlockTXs Iterable"() {
        final int NUM_TXS = 10
        given:
            println(" - Connecting to the DB...")
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .triggerBlockEvents(true)
                    .build()
        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)
            // We create and save a Block
            BlockHeader block = TestingUtils.buildBlock()
            blockStore.saveBlock(block)

            // Now we create a list of TXs and we link them to this block:
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txs.add(TestingUtils.buildTx())

            blockStore.saveBlockTxs(block.getHash(), txs)

            // Now we are going to extract them using an Iterable, and we make sure that we extract all of them, so
            // we use a Map to keep track of them;
            Map<Sha256Wrapper, Boolean> txsRead = txs
                .stream()
                .collect(Collectors.toMap({ tx -> tx.getHash()}, { h -> false}))

            blockStore.printKeys()

            Iterator<Sha256Wrapper> txsIt = blockStore.getBlockTxs(block.getHash()).iterator()
            while (txsIt.hasNext()) {
                Sha256Wrapper key = txsIt.next();
                System.out.println("Reading Key : " + key.toString());
                txsRead.put(key, true)
            }

            boolean ok = txsRead.size() == txs.size() &&
                txsRead.values().stream().filter({v -> !v}).count() == 0
        then:
            ok
        cleanup:
            blockStore.removeBlockTxs(block.getHash())
            blockStore.removeBlock(block.getHash())
            blockStore.printKeys()
            blockStore.stop()
    }

    /**
     * We test that a Tx can be linked/unlinked from/to a Block and the underlying information in the Db keeps consistent
     */
    def "testing linking and unlinking Txs from/to a Block"() {
        final int NUM_TXS = 3
        given:
            println(" - Connecting to the DB...")
            BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                    .networkId("BSV-Mainnet")
                    .build()
            BlockStoreFDB blockStore = BlockStoreFDB.builder()
                    .config(config)
                    .triggerBlockEvents(true)
                    .build()
        when:
            blockStore.start()
            //TestingUtils.clearDB(blockStore.db)
            // We save 2 Blocks:
            BlockHeader block1 = TestingUtils.buildBlock()
            BlockHeader block2 = TestingUtils.buildBlock()
            blockStore.saveBlock(block1)
            blockStore.saveBlock(block2)

            // We create several Txs and save them
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) {
                txs.add(TestingUtils.buildTx())
            }
            blockStore.saveTxs(txs)

            blockStore.printKeys()
            // No we link all of them with the FIRST Block:
            txs.forEach({tx -> blockStore.linkTxToBlock(tx.getHash(), block1.getHash())})

            blockStore.printKeys()

            // We take the FIRST Txs and we also Link it with the SECOND Block:
            Tx sharedTx = txs.get(0)
            blockStore.linkTxToBlock(sharedTx.getHash(), block2.getHash())

            blockStore.printKeys()

            // We check the Txs have been properly linked to the block 1:
            int numTxsLinkedBlock1 = blockStore.getBlockNumTxs(block1.getHash())
            boolean txsLinkedBlock1_OK = true
            List<Sha256Wrapper> txsLinkedBlock1Hashes = new ArrayList()

            // We check that all the Txs linked to this Block1 are correct (they are also present int he original
            // List of Txs...
            Iterator<Sha256Wrapper> txsLinkedBlock1It = blockStore.getBlockTxs(block1.getHash()).iterator()
            while (txsLinkedBlock1It.hasNext()) {
                Sha256Wrapper txHash = txsLinkedBlock1It.next()
                txsLinkedBlock1_OK &= !txsLinkedBlock1Hashes.contains(txHash) && txs.stream().anyMatch({tx -> tx.getHash().equals(txHash)})
                txsLinkedBlock1Hashes.add(txHash)
            }
            // And we also check that the number of Txs is correct
            txsLinkedBlock1_OK &= (numTxsLinkedBlock1 == txs.size())

            // We check that the FIRST Tx has been linked to the FIRST and SECOND Blocks:
            boolean txsLinkedBlock2_OK = true
            int numTxLinkedBlock2 = blockStore.getBlockNumTxs(block2.getHash())
            Iterator<Sha256Wrapper> txsLinkedBlock2It = blockStore.getBlockTxs(block2.getHash()).iterator()
            Sha256Wrapper txLinkedBlock2Hash = txsLinkedBlock2It.next()

            txsLinkedBlock2_OK = txLinkedBlock2Hash.equals(sharedTx.getHash()) && (numTxLinkedBlock2 == 1)

            // Now we unlink the Shared TX from all its Blocks (block1 and Block 2) and check the result:
            blockStore.unlinkTx(sharedTx.getHash())

            boolean sharedTxUnlinked_OK = true
            sharedTxUnlinked_OK &= (blockStore.getBlockNumTxs(block1.getHash()) == 2)
            sharedTxUnlinked_OK &= (blockStore.getBlockNumTxs(block2.getHash()) == 0)
            sharedTxUnlinked_OK &= (blockStore.getBlockHashLinkedToTx(sharedTx.getHash()).size() == 0)

            // Now we unlink the Block1 from all its Txs an we check the result:
            blockStore.unlinkBlock(block1.getHash())
            boolean block1Unlinked_OK = true;
            for (int i = 1; i < txs.size(); i++) {
                List<Sha256Wrapper> blocksLinked = blockStore.getBlockHashLinkedToTx(txs.get(i).getHash())
                block1Unlinked_OK &= (blocksLinked.size() == 0)
            }
            blockStore.printKeys()

        then:
            txsLinkedBlock1_OK
            txsLinkedBlock2_OK
            sharedTxUnlinked_OK
            block1Unlinked_OK
        cleanup:
            blockStore.removeTxs(txs.stream().map({tx -> tx.getHash()}).collect(Collectors.toList()))
            blockStore.removeBlock(block1.getHash())
            blockStore.removeBlock(block2.getHash())
            blockStore.printKeys()
            blockStore.stop()
    }
}
