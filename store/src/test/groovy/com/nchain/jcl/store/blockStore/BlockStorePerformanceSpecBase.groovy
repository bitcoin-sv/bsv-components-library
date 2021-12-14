package com.nchain.jcl.store.blockStore


import com.nchain.jcl.store.common.TestingUtils
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.TxBean
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors


/**
 * Testing class for Performance We are NOT testing functionality here, just making measurements of the time it
 * takes for some operations to perform.
 */
abstract class BlockStorePerformanceSpecBase extends BlockStoreSpecBase {

    @Ignore // time-consuming
    def "Testing Saving Block and Txs and link them later on "() {
        int NUM_TXS = 100_000           // Total Num of Tx inserted
        int BATCH_TXS_SIZE = 10_000;    // Num Txs saved on each call/batch
        given:
            BlockStore db = getInstance("BV-Main", false, false)
            HeaderReadOnly block = TestingUtils.buildBlock()
        when:
            db.start()
            println("Starting Performance Test...")
            println("Inserting 1 Block and " + NUM_TXS + " Txs...")

            Instant beginTime = Instant.now();
            db.saveBlock(block);
            System.out.println(" - Block saved.");
            System.out.println(" - Linking Txs to the Block...");
            List<Tx> txsToInsert = new ArrayList<>();
            for (int i = 0; i < NUM_TXS; i++) {
                txsToInsert.add(TestingUtils.buildTx());
                if ((txsToInsert.size() == BATCH_TXS_SIZE) || (i == (NUM_TXS -1))) {
                    db.saveTxs(txsToInsert);
                    System.out.println(" - " + txsToInsert.size() + " Txs saved, " + (i + 1) + " in total.");
                    db.linkTxsToBlock(txsToInsert.stream().map({tx -> tx.getHash()}).collect(Collectors.toList()), block.getHash());
                    System.out.println(" - " + txsToInsert.size() + " Txs linked to the Block, " + (i + 1) + " in total.");
                    txsToInsert.clear();
                }
            } // for...
            long time = Duration.between(beginTime, Instant.now()).toMillis();
            println(time + " millisecs to Insert 1 Block and " + NUM_TXS + " Txs...")

            //db.printKeys()

            println("Removing the Block and ALL its Txs...")
            Instant begin = Instant.now()
            db.removeBlockTxs(block.getHash())
            db.removeBlock(block.getHash())
            long millisecs = Duration.between(begin, Instant.now()).toMillis()
            println(millisecs + " millisecs.")

            println("Performance Test End.")
        then:
            db.getNumBlocks() == 0
            db.getNumTxs() == 0
        cleanup:
            db.stop()
    }

    @Ignore // time-consuming
    def "Testing Saving Block and Txs and link them at the moment of saving"() {
        int NUM_TXS = 100_000           // Total Num of Tx inserted
        int BATCH_TXS_SIZE = 10_000;    // Num Txs saved on each call/batch
        given:
            BlockStore db = getInstance("BV-Main", false, false)
            HeaderReadOnly block = TestingUtils.buildBlock()
        when:
            db.start()
            println("Starting Performance Test...")
            println("Inserting 1 Block and " + NUM_TXS + " Txs...")

            Instant beginTime = Instant.now();
            db.saveBlock(block);
            System.out.println(" - Block saved.");
            System.out.println(" - Linking Txs to the Block...");
            List<Tx> txsToInsert = new ArrayList<>();
            for (int i = 0; i < NUM_TXS; i++) {
                txsToInsert.add(TestingUtils.buildTx());
                if ((txsToInsert.size() == BATCH_TXS_SIZE) || (i == (NUM_TXS -1))) {
                    db.saveBlockTxs(block.getHash(), txsToInsert);
                    System.out.println(" - " + txsToInsert.size() + " Txs saved and linked to the Block , " + (i + 1) + " in total.");
                    txsToInsert.clear();
                }
            } // for...
            long time = Duration.between(beginTime, Instant.now()).toMillis();
            println(time + " millisecs.")

            //db.printKeys()

            println("Removing the Block and ALL its Txs...")
            Instant begin = Instant.now()
            db.removeBlockTxs(block.getHash())
            db.removeBlock(block.getHash())
            long millisecs = Duration.between(begin, Instant.now()).toMillis()
            println(millisecs + " millisecs to Remove 1 Block and " + NUM_TXS + " Txs...")

            println("Performance Test End.")
        then:
            // After the CleanUp, there will be ZERO Blocks if the DB is a BlockStore, or 1 (the GENESIS) block is
            // the DB is a BlockChainStore...
            (db.getNumBlocks() == 0) || (db.getNumBlocks() == 1)
            db.getNumTxs() == 0
        cleanup:
            db.stop()
    }

    @Ignore
    def "testing inserting individual Txs"() {
        given:
            int NUM_TXS = 1_000
            BlockStore db = getInstance("BV-Main", false, false)
        when:
            println("Performance testing: testing inserting individual Txs...")
            db.start()
            Instant begin = Instant.now()

            Tx tx = TestingUtils.buildTx()
            for (int i = 0; i < NUM_TXS; i++) {
                db.saveTx(tx)
                println("Tx " + i + " inserted")
            }
            long millisecs = Duration.between(begin, Instant.now()).toMillis()
            long txsPeSec = (NUM_TXS * 1000 ) / millisecs
            println("Operation performed in " + millisecs + " millisecs.")
            println("Performance: " + txsPeSec + " txs/sec")
        then:
            true
        cleanup:
            db.clear()
            db.stop()
    }

    @Ignore
    def "testing inserting List of Txs"() {
        given:
            int TXS_LIST_SIZE = 1_000
            int NUM_LISTS = 5
            BlockStore db = getInstance("BV-Main", false, false)
        when:
            println("Performance testing: testing inserting List of Txs...")
            db.start()
            Instant begin = Instant.now()
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < TXS_LIST_SIZE; i++) txs.add(TestingUtils.buildTx())

            for (int a = 0; a < NUM_LISTS; a++) {
                db.saveTxs(txs)
                println("List of " + txs.size() + " txs inserted")
            }
            long millisecs = Duration.between(begin, Instant.now()).toMillis()
            long txsPeSec = (NUM_LISTS * TXS_LIST_SIZE * 1000 ) / millisecs
            println("Operation performed in " + millisecs + " millisecs.")
            println("Performance: " + txsPeSec + " txs/sec")
        then:
            true
            cleanup:
            db.clear()
            db.stop()
    }

    @Ignore
    def "testing searching and inserting individual Txs"() {
        given:
            int NUM_TXS = 1_000
            BlockStore db = getInstance("BV-Main", false, false)
        when:
            println("testing searching and inserting individual Txs...")
            db.start()
            Instant begin = Instant.now()

            Tx tx = TestingUtils.buildTx()
            for (int i = 0; i < NUM_TXS; i++) {
                db.containsTx(tx.hash)
                db.saveTx(tx)
                println("Tx " + i + " searched and inserted")
            }
            long millisecs = Duration.between(begin, Instant.now()).toMillis()
            long txsPeSec = (NUM_TXS * 1000 ) / millisecs
            println("Operation performed in " + millisecs + " millisecs.")
            println("Performance: " + txsPeSec + " txs/sec")
        then:
            true
        cleanup:
            db.clear()
            db.stop()
    }

    @Ignore
    def "testing searching and inserting Lists of Txs"() {
        given:
            int TXS_LIST_SIZE = 5000
            int NUM_LISTS = 100
            BlockStore db = getInstance("BV-Main", false, false)
        when:
            println("testing searching and inserting Lists of Txs...")
            db.start()

            List<List<Tx>> txs = new ArrayList<>()
            for (int i = 0; i < NUM_LISTS ; i++) {
                println("Creating list of " + TXS_LIST_SIZE + " txs...")
                List<Tx> subList = new ArrayList<>()
                for (int b = 0; b < TXS_LIST_SIZE; b++) {
                    Tx tx = TestingUtils.buildTx();
                    byte[] txPayload = tx.serialize()
                    Tx txToInsert = new TxBean(txPayload)
                    subList.add(txToInsert)
                }
                txs.add(subList)
            }

            int currentListNum = 1;
            Instant begin = Instant.now()
            for (List<Tx> subListTxs: txs) {
                List<Tx> txsInserted = db.saveTxsIfNotExist(subListTxs)
                println("List (" + currentListNum + "/" + NUM_LISTS + ") of " + subListTxs.size() + " txs searched, " + txsInserted.size() + " txs inserted")
                currentListNum++;
            }

            long millisecs = Duration.between(begin, Instant.now()).toMillis()
            long txsPeSec = (NUM_LISTS * TXS_LIST_SIZE * 1000 ) / millisecs
            println("Operation performed in " + millisecs + " millisecs.")
            println("Performance: " + txsPeSec + " txs/sec")
            db.clear()
        then:
            db.getNumTxs() == 0
        cleanup:
            db.stop()
    }

    @Ignore
    def "testing searching and inserting Lists of Txs, all already inserted"() {
        given:
            int TXS_LIST_SIZE = 5_000
            int NUM_LISTS = 5
            BlockStore db = getInstance("BV-Main", false, false)
        when:
            println("testing searching and inserting Lists of Txs, all already inserted...")
            db.start()
            db.clear()
            List<List<Tx>> txs = new ArrayList<>()
            for (int i = 0; i < NUM_LISTS ; i++) {
                List<Tx> subList = new ArrayList<>()
                for (int b = 0; b < TXS_LIST_SIZE; b++) {
                    subList.add(TestingUtils.buildTx(TestingUtils.buildRandomHash()))
                }
                txs.add(subList)
                db.saveTxs(subList)
            }

            Instant begin = Instant.now()
            for (List<Tx> subListTxs: txs) {
                List<Tx> txsInserted = db.saveTxsIfNotExist(subListTxs)
                println("List of " + subListTxs.size() + " txs searched, " + txsInserted.size() + " txs inserted")
            }

            long millisecs = Duration.between(begin, Instant.now()).toMillis()
            long txsPeSec = (NUM_LISTS * TXS_LIST_SIZE * 1000 ) / millisecs
            println("Operation performed in " + millisecs + " millisecs.")
            println("Performance: " + txsPeSec + " txs/sec")
            db.clear()
        then:
            db.getNumTxs() == 0
        cleanup:
            db.stop()
    }

}
