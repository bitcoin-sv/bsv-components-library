package com.nchain.jcl.protocol.integration.handlers.block

import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVStnConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBTCMainConfig
import com.nchain.jcl.protocol.handlers.block.BlockDownloaderHandler
import com.nchain.jcl.protocol.handlers.block.BlockDownloaderHandlerConfig
import com.nchain.jcl.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.protocol.wrapper.P2P
import com.nchain.jcl.protocol.wrapper.P2PBuilder
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * An integration Test for Downloading Blocks.
 */
class BlockDownloadTest extends Specification {

    // Several real Block HASHES to download from different Networks:

    private static final List<String> BLOCKS_BSV_MAIN = Arrays.asList(
            "0000000000000000027abeb2a2348dac5f953676f6b68a6ed5d92458a1c12cab", // 0.6MB
            "000000000000000000dd6c89655ca27fd2555247232a5ced8376f5bda0d26ec4", // 12MB
            "000000000000000002f5268d72f9c79f29bef494e350e58f624bcf28700a1846"  // 369MB
    )
    private static final List<String> BLOCKS_BSV_STN = Arrays.asList(
           // "00000000041a389a73cfdc312f06eb1ea187b86a227b5cca5002d30ccb55e6e9", // 450MB
           // "000000000c3c309a1597f0626abaa4fa32ca0085851eceeaf56c3288be800752", // 380MB
           // "000000001ef6a2b165313202ad6938fc90ae942ad09575b6929bdf7558db78ea", // 325MB
            "0000000010366d336e351d020a838c4992878ba8f0bad3c62d1810319ff6da24"  // 192MB
    )
    private static final List<String> BLOCKS_BTC_MAIN = Arrays.asList(
            "000000000000000000067e14c07b50025455a26cd745ed32247a64ab917e677e", //1MB
            "00000000000000000007f095af6667da606d2d060f3a02a9c6a1e6a2ef9fc4e9"  // 1MB
    )

    /**
     * We test that the Blocks can be either downloaded for different Chains (BSV, BTC, etc).
     * The test is parametrized. For each Chain, we provide the specific configuration for it, and the list
     * of Block hashes to download from it. The test will finish when all the blocks are processed, because
     * they have been either downloaded or discarded, OR the tests takes longer than a THRESHOLD specified
     * (in the last case, the test will fail).
     *
     */
    // We disable this test, since it's very time-consuming

    def "Testing Block Downloading"() {
        given:
            // The longest Timeout we'll wait for to run the test:
            Duration TIMEOUT = Duration.ofMinutes(5)

            // We set up the configuration:
            BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(3)
                .maxIdleTimeout(Duration.ofSeconds(10))
                .build()
            // We configure the P2P Service:
            P2P p2p = new P2PBuilder("testing")
                .minPeers(40)
                .maxPeers(42)
                .config(config)
                .config(blockConfig)
                .publishState(BlockDownloaderHandler.HANDLER_ID, Duration.ofMillis(500))
                //.publishState(HandshakeHandler.HANDLER_ID, Duration.ofMillis(200))
                .build()

            // We are keeping track of the Blocks being downloaded:
            Map<String, BlockHeaderMsg> blockHeaders = new ConcurrentHashMap<>()    // Block Headers
            Map<String, Long> blockTxs  = new ConcurrentHashMap<>()                 // number of TXs downloaded for each Block...
            Set<String> blocksDownloaded = new HashSet<>()                          // Blocks fully downloaded
            Set<String> blocksDiscarded = new HashSet<>()                           // Blocks Discarded...

            // Every time a Header is downloaded, we store it...
            p2p.EVENTS.BLOCKS.BLOCK_HEADER_DOWNLOADED.forEach({ e ->
                String hash = e.blockHeaderMsg.hash.toString()
                blockHeaders.put(hash, e.blockHeaderMsg)
            })

            // Every time a set of TXs is downloaded, we increase the counter of Txs for this block:
            p2p.EVENTS.BLOCKS.BLOCK_TXS_DOWNLOADED.forEach({e ->
                String hash = e.blockHeaderMsg.hash.toString()
                Long currentTxs = blockTxs.containsKey(hash)? (blockTxs.get(hash) + e.txsMsg.size()) : e.txsMsg.size()
                blockTxs.put(hash, currentTxs)
                //println(Thread.activeCount() + " threads, " + currentTxs + " Txs downloaded...")
                /*
                if (blockHeaders.containsKey(hash)) {
                    Long totalTxs = blockHeaders.get(hash).transactionCount.value
                    println(" > Block " + hash + ": " + currentTxs + " Txs (of " + totalTxs + ") downloaded...")
                } else println(" > Block " + hash + ": " + currentTxs + " Txs downloaded...")

                 */
            })


            // Blocks fully downloaded and discarded
            p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->
                blocksDownloaded.add(e.blockHeader.hash.toString())
                println(" > Block " + e.blockHeader.hash.toString() + " Downloaded.")
            })
            p2p.EVENTS.BLOCKS.BLOCK_DISCARDED.forEach({ e ->
                blocksDiscarded.add(e.hash)
                println(" > Block " + e.hash + " discarded : " + e.reason)
            })

            // We log the Block Download Status:
            p2p.EVENTS.STATE.BLOCKS.forEach( {e -> println(e)})
            //p2p.EVENTS.STATE.HANDSHAKE.forEach({ e -> println(e)})

        when:
            println(" > Testing Block Download in " + config.toString() + "...")
            // WE start the Service and request to download the Blocks...
            p2p.start()
            p2p.REQUESTS.BLOCKS.download(block_hashes).submit()

            // We'll wait until all the blocks are done (downloaded or discarded) OR we've waited for too long...
            Instant startTime = Instant.now()
            boolean allBlocksDone = false
            boolean timeoutBroken = false;
            while (!allBlocksDone && !timeoutBroken) {
                Thread.sleep(500)
                allBlocksDone = (blocksDownloaded.size() + blocksDiscarded.size()) == block_hashes.size()
                timeoutBroken = Duration.between(startTime, Instant.now()).compareTo(TIMEOUT) > 0
            }
            p2p.stop()
            if (timeoutBroken) println("Timeout BROKEN!")
        then:
            allBlocksDone

        where:
            config                          |   block_hashes
           // new ProtocolBSVMainConfig()     |   BLOCKS_BSV_MAIN
            new ProtocolBSVStnConfig()      |   BLOCKS_BSV_STN
           // new ProtocolBTCMainConfig()     |   BLOCKS_BTC_MAIN
    }

}
