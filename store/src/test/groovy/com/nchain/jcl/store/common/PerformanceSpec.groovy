package com.nchain.jcl.store.common


import com.nchain.jcl.store.blockStore.BlockStore
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant


/**
 * Testing class for Performance We are NOT testing functionality here, just making measurements of the time it
 * takes for some operations to perform.
 */
abstract class PerformanceSpec extends Specification {

    abstract BlockStore getInstance(String netID, boolean triggerBlockEvents, boolean triggerTxEvents);

    @Ignore // time-consuming
    def "Testing Saving Block and Txs and linke them later on "() {
        int NUM_TXS = 100_000
        given:
            BlockStore db = getInstance("BV-Main", false, false)
            HeaderReadOnly block = TestingUtils.buildBlock()
        when:
            db.start()
            println("Starting Performance Test...")
            println("Inserting 1 Block and " + NUM_TXS + " Txs...")
            long time = TestingUtils.performanceLinkBlockAndTxs(db, block, NUM_TXS)
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
    def "Testing Saving Block and Txs and linke them at the moment of saving"() {
        int NUM_TXS = 100_000
        given:
            BlockStore db = getInstance("BV-Main", false, false)
            HeaderReadOnly block = TestingUtils.buildBlock()
        when:
            db.start()
            println("Starting Performance Test...")
            println("Inserting 1 Block and " + NUM_TXS + " Txs...")
            long time = TestingUtils.performanceSaveBlockAndTxs(db, block, NUM_TXS)
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


}
