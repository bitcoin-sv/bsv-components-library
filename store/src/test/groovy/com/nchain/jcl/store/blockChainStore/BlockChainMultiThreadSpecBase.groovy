package com.nchain.jcl.store.blockChainStore



import com.nchain.jcl.store.common.TestingUtils
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import io.bitcoinj.bitcoin.api.extended.ChainInfo
import io.bitcoinj.core.Sha256Hash

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Testing class for scenarios involving Multi Thread, with multiple processes writing and reading data at the
 * same time.
 */
abstract class BlockChainMultiThreadSpecBase extends BlockChainStoreSpecBase {

    /**
     * We test that we can have one process changing continuously the Tip of the chain while other is continuously
     * reading the TIP. the Tip of the chain read must be consistent and never be null.
     */
    def "testing Changing Tips and getTipsChain"() {
        given:
            // Configuration and DB start up:
            println(" - Connecting to the DB...")
            HeaderReadOnly genesisBlock = TestingUtils.buildBlock(Sha256Hash.ZERO_HASH.toString())
            println(" - Using block genesis: " + genesisBlock.getHash())
            BlockChainStore db = getInstance("BSV-Main", false, false, genesisBlock, Duration.ofMillis(100), null, null,         null, null)

            // We create an additional Block
            HeaderReadOnly block = TestingUtils.buildBlock(genesisBlock.hash.toString())

            // We define the time noth process will be running:
            Duration testDuration = Duration.ofSeconds(2)

            // We define the process that updates the Tip of the chain continuously, adding a removing a Block
            // from it.
            Runnable writeProcess = {  ->
                Instant now = Instant.now()
                while (Duration.between(now, Instant.now()).compareTo(testDuration) < 0) {
                    db.saveBlock(block)
                    println ("Block saved:" + block.hash.toString() + "...")
                    Thread.sleep(10)
                    db.removeBlock(block.hash)
                    println ("Block removed:" + block.hash.toString() + "...")
                    Thread.sleep(10)
                }
            }

            // We define the process that Reads the Tips of the Chain. These tips should never be null....
            AtomicBoolean error = new AtomicBoolean(false)
            Runnable readProcess = { ->
                Instant now = Instant.now()
                while (Duration.between(now, Instant.now()).compareTo(testDuration) < 0) {
                    Optional<ChainInfo> longestChain = db.getLongestChain()
                    if (longestChain == null || longestChain.isEmpty()) error.set(true)
                    else println ("Read longest Chain: " + longestChain.get().header.hash.toString())
                    Thread.sleep(5)
                }
            }

            // We init the ExecutorService to launch the Threads...
            ExecutorService executor = Executors.newFixedThreadPool(2)

        when:
            db.start()
            // We clean the DB:
            db.clear()
            // We check the DB Content in the console...
            println("Content of DB Right BEFORE the Test:")
            db.printKeys()

            executor.submit(writeProcess)
            executor.submit(readProcess)
            executor.awaitTermination(testDuration.toMillis() * 2, TimeUnit.MILLISECONDS)
            executor.shutdownNow()
            db.clear()
            db.stop()
        then:
            !error.get()
    }
}
