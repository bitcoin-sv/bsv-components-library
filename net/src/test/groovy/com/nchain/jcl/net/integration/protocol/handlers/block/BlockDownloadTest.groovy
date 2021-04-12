package com.nchain.jcl.net.integration.protocol.handlers.block

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandler
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An integration Test for Downloading Blocks.
 */
class BlockDownloadTest extends Specification {

    // Several real Block HASHES to download from different Networks:

    private static final List<String> BLOCKS_BSV_MAIN = Arrays.asList(
            "0000000000000000027abeb2a2348dac5f953676f6b68a6ed5d92458a1c12cab", // 0.6MB
            "000000000000000000dd6c89655ca27fd2555247232a5ced8376f5bda0d26ec4", // 12MB
            "000000000000000002f5268d72f9c79f29bef494e350e58f624bcf28700a1846",  // 369MB

            "0000000000000000071e6e1c401fc530a63d27c826661a2f48709ba2ab51ecb4", // 7K
            "0000000000000000010b0c201f99c4636b35972fc870cdd322d49aea4e9e469e", // 4MB
            "00000000000000000dcb5ea5c87f337d017c077e10e314cb0176026266faef0c", // 17MB
            "0000000000000000039f4868d8c88d8ba86458101b965f5885cc63ed6814fb5c", // 2MB
            "00000000000000000c6e3e84fcf44f0305a2628d07bc082fd9885480c4ea0eb0", // 71MB
            "000000000000000002a8d922a4e1d365019758af5e9a2260f6cea0261d459b38", // 63MB
            "00000000000000000b03fc7421e1063e1f55e7a383801debc551daf6d37c3fa8", // 610KB
            "000000000000000009564c0360e55b125af1327eaf56b6b7566493112523437b", // 130KB
            "00000000000000000249f0276b4535875b497c42a737ce477b5c6e11ff55fcd3", // 9MB
            "0000000000000000068cd5441b2f406562939988bfeafecfe9a90949c055ac78", // 1.6MB
            "000000000000000001e6ddb4461940f0bedf69c7b63a239f33d641c9f8e4ac73", // 1.4MB
            "0000000000000000032b2199242e73a7a1388f036648c49856d9d39b5e44d1b0", // 1.8MB
            "0000000000000000099ae699f1b233a5a5f8a33ce2a570850a86839dd831e4a6", // 840KB
            "0000000000000000031036042beb5a20085466f4640a61026ce849be411255dd", // 81KB
            "000000000000000009b7423dc3ec7a97c6593bda228fcdce17e967f0e1729412", // 84MB
            "0000000000000000007ef109b1266d8701d15158e6795b7d8f2080ebecf3acaf", // 16MB
            "00000000000000000b53ca866c42c077d95b3d735df593229fd19f49352f5f80", // 10KB
            "00000000000000000b0f7d16e33e66b64bf94bb5c6543f3b680ce9d7162fef21", // 1.7MB
            "0000000000000000061757aed9f19d4e6a94ad5f309d1cc53f4303298cbf033f", // 2.2MB


    )
    private static final List<String> BLOCKS_BSV_STN = Arrays.asList(
            //"00000000041a389a73cfdc312f06eb1ea187b86a227b5cca5002d30ccb55e6e9", // 450MB
            //"000000000c3c309a1597f0626abaa4fa32ca0085851eceeaf56c3288be800752", // 380MB
            //"000000001ef6a2b165313202ad6938fc90ae942ad09575b6929bdf7558db78ea", // 325MB
            //"0000000010366d336e351d020a838c4992878ba8f0bad3c62d1810319ff6da24"  // 192MB
            "000000000f07389d9ed5e0d31e1f93dce1dd4777ae5f204272ce4beb20c838f7", // 5MB
            "00000000164f38c92e48c04cb7ce0bc9a26faf7651f95cb2cc349a4a4e100dae", // 224 bytes
            "000000001bc7963fd54d4968a68a3b467564a4e05f357a9ff5e53fd422362d6e"  // 224 bytes

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
    //@Ignore
    def "Testing Block Downloading"() {
        given:
            // The longest Timeout we'll wait for to run the test:
            Duration TIMEOUT = Duration.ofMinutes(5)

            // Basic Config:
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(10))
                .maxPeers(OptionalInt.of(15))
                .build()

            // We set up the Download configuration:
            BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(10)
                .maxIdleTimeout(Duration.ofSeconds(10))
                .build()

            // We configure the P2P Service:
            P2P p2p = new P2PBuilder("testing")
                .config(config)
                .config(basicConfig)
                .config(blockConfig)
                .publishState(BlockDownloaderHandler.HANDLER_ID, Duration.ofMillis(500))
                //.publishState(HandshakeHandler.HANDLER_ID, Duration.ofMillis(200))
                .build()

            // We are keeping track of the Blocks being downloaded:
            Map<String, BlockHeaderMsg> blockHeaders = new ConcurrentHashMap<>()    // Block Headers
            Map<String, Long> blockTxs  = new ConcurrentHashMap<>()                 // number of TXs downloaded for each Block...
            Set<String> blocksDownloaded = new HashSet<>()                          // Blocks fully downloaded
            Set<String> blocksDiscarded = new HashSet<>()                           // Blocks Discarded...

            // We capture the state when we reach the min Peers, so we do not start the download until this moment:
            AtomicBoolean connReady = new AtomicBoolean(false);
            p2p.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({e -> connReady.set(true)})

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
            })

            // Blocks fully downloaded
            p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->
                blocksDownloaded.add(e.blockHeader.hash.toString())
                println(" > Block " + e.blockHeader.hash.toString() + "(" + e.getBlockSize() + " bytes) Downloaded.")
            })

            // Blocks discarded
            p2p.EVENTS.BLOCKS.BLOCK_DISCARDED.forEach({ e ->
                blocksDiscarded.add(e.hash)
                println(" > Block " + e.hash + " discarded : " + e.reason)
            })

            // We log the Block Download Status:
            p2p.EVENTS.STATE.BLOCKS.forEach( {e -> println(e)})
            p2p.EVENTS.STATE.HANDSHAKE.forEach({ e -> println(e)})

        when:
            println(" > Testing Block Download in " + config.toString() + "...")
            // WE start the Service and request to download the Blocks...
            p2p.start()
            // we wait until we reach the minimum number of Peers:
            while (!connReady.get()) Thread.sleep(100)

            println("Connection Ready...")
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
            Duration testDuration = Duration.between(startTime, Instant.now())
            println("It took " + testDuration.toSeconds() + " seconds to finish the Test.")
            if (timeoutBroken) println("Test Timeout BROKEN!")
        then:
            allBlocksDone

        where:
            config                     |   block_hashes
            new ProtocolBSVMainConfig()      |   BLOCKS_BSV_MAIN
           //com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder.get(MainNetParams.get()) |   BLOCKS_BSV_MAIN
           //new ProtocolBSVStnConfig()      |   BLOCKS_BSV_STN
           //new ProtocolBTCMainConfig() |   BLOCKS_BTC_MAIN
    }

}
