package com.nchain.jcl.store.levelDB.blockStore

import com.google.common.io.RecursiveDeleteOption
import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.common.PaginatedRequest
import com.nchain.jcl.store.common.PaginatedResult
import com.nchain.jcl.store.levelDB.common.TestingUtils
import spock.lang.Ignore
import spock.lang.Specification


import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.io.MoreFiles

import java.util.stream.Collectors

/**
 * A Test class for Basic Scenarios (CRUD). We test that the DB state is consistent after the CRUD operations, and
 * the Events are triggered.
 */
class BlockStoreBasicSpec extends Specification {



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

    /**
     * We test that TXs are properly saved and removed into the DB and the related Events are properly triggered.
     */
    def "testing saving/removing Txs"() {
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

            db.EVENTS().TXS_SAVED.forEach({e -> numTxsStoredEvents.incrementAndGet()})
            db.EVENTS().TXS_REMOVED.forEach({e -> numTxsRemovedEvents.incrementAndGet()})

        when:
            // We save one Tx, and then we retrieve it:
            Tx tx1 = TestingUtils.buildTx()
            db.saveTx(tx1)
            Tx txInserted = db.getTx(tx1.getHash()).get()

            // We check that the Block retrieved is actually the one we inserted:
            boolean found = txInserted != null && txInserted.equals(tx1)

            // Now we remove it and we check that it's been actually removed:
            db.removeTx(tx1.getHash())
            boolean removedAfter = db.getTx(tx1.getHash()).isEmpty()

            // We do the same here, but using the methods that accept a List of objects

            Tx tx2 = TestingUtils.buildTx()
            db.saveTxs(Arrays.asList(tx2))
            db.removeTxs(Arrays.asList(tx2.getHash()))
            // We wait a bit, to give the events enought time to reach their callbacks:
            Thread.sleep(100)
        then:
            found
            removedAfter
            numTxsStoredEvents.get() == 2
            numTxsRemovedEvents.get() == 2
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * We test that Blocks and Txs are stored and loined properly, so we can retrieve the Txs belonging to that
     * Block in a paginated way.
     */
    def "Testing saving/removing Txs within a Block"() {

        final int NUM_BLOCKS = 2      // Num of Blocks
        final int NUM_TXS = 3        // Num of TXs to insert within each block
        final int TXS_PAGESIZE = 1    // 500 TXs read in each Paginated Result

        given:
            // Configuration and DB start up:
            Path dbPath = Path.of(TestingUtils.buildWorkingFolder())
            BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                    .workingFolder(dbPath)
                    .build()
            BlockStore db = BlockStoreLevelDB.builder()
                    .config(dbConfig)
                    .triggerBlockEvents(true)
                    .triggerTxEvents(true)
                    .build()

            // for keeping track of the Events triggered:
            AtomicInteger numBlockStoredEvents = new AtomicInteger()
            AtomicInteger numTxsStoredEvents = new AtomicInteger()

            db.EVENTS().BLOCKS_SAVED.forEach({ e -> numBlockStoredEvents.incrementAndGet()})
            db.EVENTS().TXS_SAVED.forEach({e -> numTxsStoredEvents.incrementAndGet()})

        when:
            boolean TEST_OK = true // An time we detect somthing wrong, we set this to FALSE and Skip the test

            // We create and save a Block, and we create Txs linked to it:
            for (int i = 0; i < NUM_BLOCKS; i++) {
                BlockHeader blockHeader = TestingUtils.buildBlock()
                db.saveBlock(blockHeader)

                // We create and save a list of TXS linked to this Block:
                List<Tx> txs = new ArrayList<>()
                for (int b = 0; b < NUM_TXS; b++) txs.add(TestingUtils.buildTx())
                println("Saving Block " + blockHeader.getHash() + ":")
                txs.forEach({tx -> println(" - tx " + tx.getHash())})
                db.saveBlockTxs(blockHeader.getHash(), txs)

                // Now we test that we can recover the Txs in a paginated fashion. We check that we can retrieve ALL the
                // Txs, and also that they are the SAME TXs (we compare their Hashes). The Txs in the DB might not be
                // inserted in the same order as they'e been created, since KEys follow a lexicograph order in LevelDB. So
                // what we'll do is to check every Tx recovered, and check that its HASH belongs to the Txs saved.

                // Map initialized with ALL the Tx Hashes created and saved in the DB
                //  - key: Tx HASH,
                //  - value: TRUE (Hash has been already read from the DB), FALSE (otherwise)
                Map<Sha256Wrapper, Boolean> txHashesRead = txs.stream()
                        .collect(Collectors.toMap({tx -> tx.getHash()},{tx -> false}))

                // We recover the TXs from this Block in a paginated way and we check:

                int currentPage = 0
                while (TEST_OK) {
                    PaginatedRequest pagReq = PaginatedRequest.builder().numPage(currentPage).pageSize(TXS_PAGESIZE).build()
                    PaginatedResult<String> result = db.getBlockTxs(blockHeader.getHash(), pagReq)
                    if (result == null || result.getResults().size() == 0) TEST_OK = false
                    else {
                        List<String> txHashesResult = result.getResults()
                        TEST_OK = txHashesResult.stream().allMatch({h ->  txHashesRead.containsKey(h) || !txHashesRead.get(h)})
                        if (TEST_OK) txHashesResult.forEach({h -> txHashesRead.put(h, true)})
                    }
                    currentPage++
                } // while...
                // We check that all the Txs have been read from the DB (there isno Txs in the HASh which value is FALSE)
                TEST_OK = txHashesRead.values().stream().filter({v -> !v}).count() == 0
                if (!TEST_OK) break
            } // for .. blocks...

        then:
            TEST_OK
            // We also test the events have being triggered
            numBlockStoredEvents.get() == NUM_BLOCKS   // one event per Block
            numTxsStoredEvents.get() == NUM_BLOCKS     // one event per List of TXs
        cleanup:
            MoreFiles.deleteRecursively(dbPath, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * We test that the TxsRemovedEvent are triggered in a paginated way when we remove all the TCs within a Block.
     */
    def "testing RemoveBlockTxs and Events Triggered"() {

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
            db.EVENTS().TXS_REMOVED.forEach({e -> numTxsRemovedEvents.incrementAndGet()})

        when:
            // We create a Block and insert TXs into it
            BlockHeader blockHeader = TestingUtils.buildBlock()
            db.saveBlock(blockHeader)
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) txs.add(TestingUtils.buildTx())

            // We also measure the time it takes to insert all these TXs:
            Instant initTime = Instant.now()
            db.saveBlockTxs(blockHeader.getHash(), txs)
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

}
