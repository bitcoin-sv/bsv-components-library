package com.nchain.jcl.store.levelDB.blockStore

import com.google.common.io.RecursiveDeleteOption
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockStore.BlockStore

import com.nchain.jcl.store.levelDB.common.TestingUtils
import spock.lang.Specification


import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.io.MoreFiles

import java.util.stream.Collectors

/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
class BlockStoreLinkSpec extends Specification {


    /**
     * We test that the TxsRemovedEvent are triggered in a paginated way when we remove all the TCs within a Block.
     */
    def "testing Save/RemoveBlockTxs and Events Triggered"() {

        final int NUM_TXS = 20_000

        // This is the DEFAULT page size for the TX in the Events Triggered by the BlockStoreLevelDB Implementation
        // (Make sure that the value defined there and this value are the same)
        final int TX_PAG_SIZE_DEFAULT = 10_000

        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .workingFolder(dbPath)
                    .build()
            BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerTxEvents(true)
                    .build()

            // for keeping track of the Events triggered:
            AtomicInteger numTxsStoredEvents = new AtomicInteger()
            AtomicInteger numTxsRemovedEvents = new AtomicInteger()

            db.EVENTS().TXS_SAVED.forEach({ e -> numTxsStoredEvents.incrementAndGet()})
            db.EVENTS().TXS_REMOVED.forEach({e ->
                numTxsRemovedEvents.incrementAndGet()
                println("> TxsRemovedEvent :: " + e.txHashes.size() + " Txs received.")
            })

        when:
            // We create a Block and insert TXs into it
            BlockHeader blockHeader = TestingUtils.buildBlock()
            db.saveBlock(blockHeader)
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) {
                txs.add(TestingUtils.buildTx())
            }

            // We also measure the time it takes to insert all these TXs:
            Instant initTime = Instant.now()
            db.saveBlockTxs(blockHeader.getHash(), txs)
            //db.printKeys()
            println(Duration.between(initTime, Instant.now()).toMillis() + " millsecs to insert and link " + NUM_TXS + " Txs")
            // Now we remove the TXs just saved for this block.
            // We also measure the time here
            initTime = Instant.now()
            db.removeBlockTxs(blockHeader.getHash())
            println(Duration.between(initTime, Instant.now()).toMillis() + " millsecs to remove and unlink " + NUM_TXS + " Txs")

        // We give it some time for the event callbacks to be triggered:
            Thread.sleep(100)
        then:
            numTxsStoredEvents.get() ==   1
            numTxsRemovedEvents.get() ==  ((NUM_TXS) / TX_PAG_SIZE_DEFAULT)
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }


    /**
     * We test that a Tx can be linked to more than 1 Block
     */
    def "Testing linking Tx to several Blocks"() {
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .workingFolder(dbPath)
                    .build()
            BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerTxEvents(true)
                    .build()
        when:
            db.start()

            // We create several blocks and save them;
            BlockHeader block1 = TestingUtils.buildBlock()
            BlockHeader block2 = TestingUtils.buildBlock()
            db.saveBlocks(Arrays.asList(block1, block2))

            // We create 4 Txs and link them so, both Blocks have one Tx in common:
            Tx tx1 = TestingUtils.buildTx()
            Tx tx2 = TestingUtils.buildTx()
            Tx tx3 = TestingUtils.buildTx()
            Tx tx4 = TestingUtils.buildTx()
            // Both Blocks have Tx2 in common:
            db.saveBlockTxs(block1.getHash(), Arrays.asList(tx1, tx2))
            db.saveBlockTxs(block2.getHash(), Arrays.asList(tx2, tx3, tx4))



            List<Sha256Wrapper> blocksFromTx1 = db.getBlockHashLinkedToTx(tx1.getHash())
            List<Sha256Wrapper> blocksFromTx2 = db.getBlockHashLinkedToTx(tx2.getHash())
            List<Sha256Wrapper> blocksFromTx3 = db.getBlockHashLinkedToTx(tx3.getHash())
            List<Sha256Wrapper> blocksFromTx4 = db.getBlockHashLinkedToTx(tx4.getHash())


            boolean OKafterSavingAndLinking = (
                    blocksFromTx1.size() == 1 &&
                    blocksFromTx2.size() == 2 &&
                    blocksFromTx3.size() == 1 &&
                    blocksFromTx4.size() == 1
            )
            db.stop()
        then:
            OKafterSavingAndLinking
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * We test the Iterable returned when we get all the Txs belonging to 1 block
     */
    def "testing BlockTXs Iterable"() {
        final int NUM_TXS = 100
        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .workingFolder(dbPath)
                    .build()
            BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerTxEvents(true)
                    .build()
        when:
            db.start()
            // We create and save a Block
            BlockHeader block = TestingUtils.buildBlock()
            db.saveBlock(block)

            // Now we create a list of TXs and we link them to this block:
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txs.add(TestingUtils.buildTx())

            db.saveBlockTxs(block.getHash(), txs)

            // Now we are going to extract them using an Iterable, and we make sure that we extract all of them, so
            // we use a Map to keep track of them;
            Map<Sha256Wrapper, Boolean> txsRead = txs
                    .stream()
                    .collect(Collectors.toMap({tx -> tx.getHash()}, {h -> false}))

            Iterator<Sha256Wrapper> txsIt = db.getBlockTxs(block.getHash()).iterator()
            while (txsIt.hasNext()) txsRead.put(txsIt.next(), true)

            boolean ok = txsRead.size() == txs.size() &&
                         txsRead.values().stream().filter({v -> !v}).count() == 0
            db.stop()
        then:
            ok
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

}
