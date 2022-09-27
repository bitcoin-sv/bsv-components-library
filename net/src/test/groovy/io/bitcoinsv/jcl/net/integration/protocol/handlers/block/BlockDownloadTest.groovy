package io.bitcoinsv.jcl.net.integration.protocol.handlers.block

import io.bitcoinsv.jcl.net.network.config.NetworkConfig
import io.bitcoinsv.jcl.net.network.config.provided.NetworkDefaultConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion
import io.bitcoinsv.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerState
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlocksDownloadHistory
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.jcl.tools.config.RuntimeConfig
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault
import io.bitcoinsv.jcl.tools.thread.ThreadUtils
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.jcl.tools.util.EventsHistory
import spock.lang.Ignore
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
    // For each Network, we Define:
    // - a list of Blocks to Download
    // - a sublist of the preivous blocks that will be cancelled after the download starts (a few seconds later)

    // BSN REGTEST
    private static final List<String> BLOCKS_BSV_REGTEST = Arrays.asList(
            "343d9ac5a0fb8b9f15077a7b00aebaed07f1bcc89cc605e8afdc5d346bb83cce" // Jakka
    )
    private static final List<String> BLOCKS_BSV_REGTEST_TO_CANCEL = new ArrayList<>();

    // BSV MAINNET:

    private static final List<String> BLOCKS_BSV_MAIN = Arrays.asList(
 //           "0000000000000000007e2d666ce2045725dda523f744ba426e7900448d71929a", // 340MB
            "0000000000000000053be5a950485a7d437aaea310dcbe4b63de4b5046928a69", // 49MB
            "0000000000000000053be5a950485a7d437aaea310dcbe4b63de4b5046928a5a", // 49MB
            "0000000011139d059a772fb14123a0bbe66b1a6782d4aebe2a6b2c9f92850a7d", // 6MB
            "00000000000000000fc65b3827b997cbce18350b7aa03ac306367e220cb7ad52", // 115MB
           "0000000000000000052c4236c4c34dc7686f8285e2646a584785b8d3b1eb8779", // 1.25GB
            "000000000000000002f5268d72f9c79f29bef494e350e58f624bcf28700a1846" // 369MB
//            "0000000000000000027abeb2a2348dac5f953676f6b68a6ed5d92458a1c12cab", // 0.6MB
//            "000000000000000000dd6c89655ca27fd2555247232a5ced8376f5bda0d26ec4", // 12MB
//
//            "0000000000000000071e6e1c401fc530a63d27c826661a2f48709ba2ab51ecb4", // 7K
//            "0000000000000000010b0c201f99c4636b35972fc870cdd322d49aea4e9e469e", // 4MB
//            "00000000000000000dcb5ea5c87f337d017c077e10e314cb0176026266faef0c", // 17MB
//            "0000000000000000039f4868d8c88d8ba86458101b965f5885cc63ed6814fb5c", // 2MB
//            "00000000000000000c6e3e84fcf44f0305a2628d07bc082fd9885480c4ea0eb0", // 71MB
//            "000000000000000002a8d922a4e1d365019758af5e9a2260f6cea0261d459b38", // 63MB
//            "00000000000000000b03fc7421e1063e1f55e7a383801debc551daf6d37c3fa8", // 610KB
//            "000000000000000009564c0360e55b125af1327eaf56b6b7566493112523437b", // 130KB
//            "00000000000000000249f0276b4535875b497c42a737ce477b5c6e11ff55fcd3", // 9MB
//            "0000000000000000068cd5441b2f406562939988bfeafecfe9a90949c055ac78", // 1.6MB
//            "000000000000000001e6ddb4461940f0bedf69c7b63a239f33d641c9f8e4ac73", // 1.4MB
//            "0000000000000000032b2199242e73a7a1388f036648c49856d9d39b5e44d1b0", // 1.8MB
//            "0000000000000000099ae699f1b233a5a5f8a33ce2a570850a86839dd831e4a6", // 840KB
//            "0000000000000000031036042beb5a20085466f4640a61026ce849be411255dd", // 81KB
//            "000000000000000009b7423dc3ec7a97c6593bda228fcdce17e967f0e1729412", // 84MB
//            "0000000000000000007ef109b1266d8701d15158e6795b7d8f2080ebecf3acaf", // 16MB
//            "00000000000000000b53ca866c42c077d95b3d735df593229fd19f49352f5f80", // 10KB
//            "00000000000000000b0f7d16e33e66b64bf94bb5c6543f3b680ce9d7162fef21", // 1.7MB
//            "0000000000000000061757aed9f19d4e6a94ad5f309d1cc53f4303298cbf033f" // 2.2MB
    )

    private static final List<String> BLOCKS_BSV_MAIN_TO_CANCEL = Arrays.asList(
            "0000000000000000071e6e1c401fc530a63d27c826661a2f48709ba2ab51ecb4", // 7K
            "0000000000000000039f4868d8c88d8ba86458101b965f5885cc63ed6814fb5c" // 2MB
           // "000000000000000002f5268d72f9c79f29bef494e350e58f624bcf28700a1846" //  369MB
    )

    // BSV STNNET:

    private static final List<String> BLOCKS_BSV_STN = Arrays.asList(
            "000000000cec34e0b122af3ff9cf3c0cca93c59fd997476528101b7f92a9b561", // 1.5MB
            "0000000014282c4cb6875e0ad41ecd269f6e3b87e870dd5e177ae6324d32a8ae", // 26 MB
        //    "00000000041a389a73cfdc312f06eb1ea187b86a227b5cca5002d30ccb55e6e9", // 450MB
        //    "000000000c3c309a1597f0626abaa4fa32ca0085851eceeaf56c3288be800752", // 380MB
        //    "000000001ef6a2b165313202ad6938fc90ae942ad09575b6929bdf7558db78ea", // 325MB
        //    "0000000010366d336e351d020a838c4992878ba8f0bad3c62d1810319ff6da24",  // 192MB
        //    "000000000f07389d9ed5e0d31e1f93dce1dd4777ae5f204272ce4beb20c838f7", // 5MB
        //    "00000000164f38c92e48c04cb7ce0bc9a26faf7651f95cb2cc349a4a4e100dae", // 224 bytes
        //    "000000001bc7963fd54d4968a68a3b467564a4e05f357a9ff5e53fd422362d6e"  // 224 bytes

    )

    private static final List<String> BLOCKS_BSV_STN_TO_CANCEL = new ArrayList<>();

    // BTC MAIN:
    private static final List<String> BLOCKS_BTC_MAIN = Arrays.asList(
            "000000000000000000067e14c07b50025455a26cd745ed32247a64ab917e677e", //1MB
            "00000000000000000007f095af6667da606d2d060f3a02a9c6a1e6a2ef9fc4e9"  // 1MB
    )

    private static final List<String> BLOCKS_BTC_MAIN_TO_CANCEL = new ArrayList<>();

    /**
     * We test that the Blocks can be either downloaded for different Chains (BSV, BTC, etc).
     * The test is parametrized. For each Chain, we provide the specific configuration for it, and the list
     * of Block hashes to download from it. The test will finish when all the blocks are processed, because
     * they have been either downloaded or discarded, OR the tests takes longer than a THRESHOLD specified
     * (in the last case, the test will fail).
     *
     */
    // We disable this test, since it's very time-consuming
    @Ignore
    def "Testing Block Downloading"() {
        given:
            // The longest Timeout we'll wait for to run the test:
            Duration TIMEOUT = Duration.ofMinutes(10)

            // Time to wait to cancel blocks after starting downloading:
            Duration WAIT_FOR_CANCELLING = Duration.ofSeconds(2);

            // Runtime Config:
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                .msgSizeInBytesForRealTimeProcessing(5_000_000)
                //  .msgSizeInBytesForRealTimeProcessing(5_000)
                .build()

            // Network Config:
            NetworkConfig networkConfig = new NetworkDefaultConfig().toBuilder()
                .maxSocketConnectionsOpeningAtSameTime(50)
                .build();

            // Basic Config:
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(1))
                .maxPeers(OptionalInt.of(10))
                .protocolVersion(ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                .build()

            // Serialization Config:
            MessageHandlerConfig messageConfig = config.getMessageConfig().toBuilder()
                .rawTxsEnabled(true)
                .verifyChecksum(true)

                .build();

            // Discovery Config:
            // This might be needed if the DNS's do not work
            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                    .addInitialConnection("144.76.117.158:8333")
                    .addInitialConnection("65.108.132.250:8333")
                    .addInitialConnection("23.250.18.170:8333")
                    .addInitialConnection("3.69.24.55:8333")
                    .addInitialConnection("95.216.243.249:8333")
                    .addInitialConnection("3.120.175.133:8333")
                    .addInitialConnection("139.59.35.196:8333")
                    .addInitialConnection("95.217.197.54:8333")
                .build()

            // We set up the Download configuration:
            BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(3)
                .maxDownloadAttempts(100)
                .maxDownloadTimeout(Duration.ofMinutes(20))
                .maxIdleTimeout(Duration.ofSeconds(10))
                .removeBlockHistoryAfterDownload(false)
                .removeBlockHistoryAfter(Duration.ofMinutes(10))
                .build()

            // We configure the P2P Service:
            P2P p2p = new P2PBuilder("testing")
                .config(runtimeConfig)
                .config(networkConfig)
                .config(config)
                .config(basicConfig)
                .config(messageConfig)
                .config(blockConfig)
                .config(discoveryConfig)
                .publishStates(Duration.ofMillis(500))
                .build()

            // We are keeping track of the Blocks being downloaded:
            Map<String, BlockHeaderMsg> blockHeaders = new ConcurrentHashMap<>()    // Block Headers
            Map<String, Long> blockTxs  = new ConcurrentHashMap<>()                 // number of TXs downloaded for each Block...
            Map<String, Long> blockTxsBytes = new ConcurrentHashMap<>()             // number of BYTES of Txs downloaded for each Block...
            Set<String> blocksDownloaded = new HashSet<>()                          // Blocks fully downloaded
            Set<String> blocksDiscarded = new HashSet<>()                           // Blocks Discarded...
            Set<String> blocksCancelled = new HashSet<>()                           // Blocks Cancelled...

            // We keep track of the last Download STATUS:
            BlockDownloaderHandlerState downloadState = null;

            // We capture the state when we reach the min Peers, so we do not start the download until this moment:
            AtomicBoolean connReady = new AtomicBoolean(false);
            p2p.EVENTS.PEERS.HANDSHAKED_MAX_REACHED.forEach({e ->
                println("Max Number of Peers reached.")
                connReady.set(true)
            })


            // Every time a Header is downloaded, we store it...
            p2p.EVENTS.BLOCKS.BLOCK_HEADER_DOWNLOADED.forEach({ e ->
                String hash = Utils.HEX.encode(Utils.reverseBytes(e.getBtcMsg().body.getBlockHeader().getHash().getBytes()))
                blockHeaders.put(hash, e.getBtcMsg().getHeader())
            })

            // Every time a set of TXs is downloaded, we increase the counter of Txs for this block:
            p2p.EVENTS.BLOCKS.BLOCK_TXS_DOWNLOADED.forEach({e ->
                String hash = Utils.HEX.encode(Utils.reverseBytes(e.getBtcMsg().body.getBlockHeader().getHash().getBytes()))
                Long currentTxs = blockTxs.containsKey(hash)? (blockTxs.get(hash) + e.getBtcMsg().body.getTxs().size()) : e.getBtcMsg().body.getTxs().size()
                blockTxs.put(hash, currentTxs)
            })

            // Every time a set of RAW TXs is downloaded, we increase the counter of Txs for this block:
            p2p.EVENTS.BLOCKS.BLOCK_RAW_TXS_DOWNLOADED.forEach({ e ->
                String hash = Utils.HEX.encode(Utils.reverseBytes(e.getBtcMsg().body.getBlockHeader().getHash().getBytes()))
                Long currentTxsBytes = blockTxsBytes.containsKey(hash)
                        ? (blockTxsBytes.get(hash) + e.getBtcMsg().body.getTxs().stream().mapToInt({tx -> (int) tx.getLengthInBytes()}).sum())
                        : e.getBtcMsg().body.getTxs().stream().mapToInt({tx -> (int) tx.getLengthInBytes()}).sum()
                println(currentTxsBytes + " bytes of Txs downloaded of the block " + hash + " from " + e.getPeerAddress());
                blockTxs.put(hash, currentTxsBytes)
            })

            // Blocks fully downloaded
            p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->
                String hash = Utils.HEX.encode(Utils.reverseBytes(e.getBlockHeader().getHash().getBytes()))
                blocksDownloaded.add(hash)
                println(" > Block " + e.blockHeader.hash.toString() + "(" + e.getBlockSize() + " bytes) Downloaded.")
            })

            // Blocks discarded
            p2p.EVENTS.BLOCKS.BLOCK_DISCARDED.forEach({ e ->
                blocksDiscarded.add(e.hash)
                println(" > Block " + e.hash + " discarded : " + e.reason)
            })

            // We log some Status:
            p2p.EVENTS.STATE.NETWORK.forEach( {e ->
                println(e.toString() + ": " + ThreadUtils.getThreadsInfo())
            })
            p2p.EVENTS.STATE.HANDSHAKE.forEach({ e -> println(e)})
            p2p.EVENTS.STATE.DISCOVERY.forEach({ e -> println(e)})
            p2p.EVENTS.STATE.BLOCKS.forEach( {e ->
                println(e)
                downloadState = (BlockDownloaderHandlerState) e.getState();
                blocksCancelled = downloadState.getCancelledBlocks()
            })

        when:
            println(" > Testing Block Download in " + config.toString() + "...")

            // We start the Service and request to download the Blocks...
            p2p.start()

            // We do NOT start downloading until we reach the MAX Number of Peers:
            while (!connReady.get()) Thread.sleep(100)

            p2p.REQUESTS.BLOCKS.download(block_hashes).submit()

            // Connections are Ready. We submit the Request to start downloading...
            println("Connection Ready...")

            // We wait some time and then we submit a request to Cancel some Blocks from downloading:
            Thread.sleep(WAIT_FOR_CANCELLING.toMillis());
            p2p.REQUESTS.BLOCKS.cancelDownload(block_hashes_to_cancel).submit()

            // We'll wait until all the blocks are done (downloaded, discarded or cancelled) OR we've waited for too long...
            Instant startTime = Instant.now()
            boolean allBlocksDone = false
            boolean timeoutBroken = false;
            Duration testDuration = null
            while (!allBlocksDone && !timeoutBroken) {
                Thread.sleep(500)
                allBlocksDone = (blocksDownloaded.size() + blocksDiscarded.size() + blocksCancelled.size()) == block_hashes.size()
                testDuration = Duration.between(startTime, Instant.now())
                timeoutBroken = testDuration.compareTo(TIMEOUT) > 0
            }
            p2p.stop()
            println("It took " + testDuration.toSeconds() + " seconds to finish the Test.")
            if (timeoutBroken) println("Test Timeout BROKEN!")

            Thread.sleep(1000)

            // Now we show the History of ALL the blocks:
            println(" TEST DONE. Printing the Whole Download Hisatory:\n");
            Map<String, EventsHistory.HistoricItem> history = downloadState.blocksHistory;
            for (String blockHash: history.keySet()) {
                println(" > block " + blockHash + " :");
                for (EventsHistory.HistoricItem historicItem : history.get(blockHash)) {
                    println("  - " + historicItem.toString());
                }
            }

        then:
            allBlocksDone

        where:
            config                     |   block_hashes     | block_hashes_to_cancel
           new ProtocolBSVMainConfig()      |   BLOCKS_BSV_MAIN    | BLOCKS_BSV_MAIN_TO_CANCEL
           //ProtocolConfigBuilder.get(RegTestParams.get()) | BLOCKS_BSV_REGTEST | BLOCKS_BSV_REGTEST_TO_CANCEL
           //io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder.get(MainNetParams.get()) |   BLOCKS_BSV_MAIN
           //new ProtocolBSVStnConfig() |   BLOCKS_BSV_STN | BLOCKS_BSV_MAIN_TO_CANCEL
           //new ProtocolBTCMainConfig() |   BLOCKS_BTC_MAIN
    }

}
