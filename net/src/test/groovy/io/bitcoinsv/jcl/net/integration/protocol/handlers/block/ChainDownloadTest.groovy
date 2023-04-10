package io.bitcoinsv.jcl.net.integration.protocol.handlers.block

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.jcl.net.integration.utils.IntegrationUtils
import io.bitcoinsv.jcl.net.network.config.NetworkConfig
import io.bitcoinsv.jcl.net.network.config.provided.NetworkDefaultConfig
import io.bitcoinsv.jcl.net.network.handlers.NetworkHandler
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandler
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerState
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandler
import io.bitcoinsv.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.GetHeadersMsg
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder

import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors

/**
 * A testing class for downloading a partial part of the Blockchain, including both Blocks and Txs.
 * You specify a Block hASH as a starting point, and JCL will start downloading the HEADERS and the Blocks until
 * the Test finishes, or you reach the Tip
 */
class ChainDownloadTest extends Specification {

    // Starting Point:
    String HASH_LOCATOR = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f" // GENESIS
    //private static final HASH_LOCATOR = "000000000000000004ea72f7d8ba1509868a0c8a852509eacd600dce7232075b";

    // Maximum Duration of the Test:
    private static final Duration TIMEOUT = Duration.ofMinutes(15)

    // If this TIMEOUT IS specified (1= null), then JCL will PAUSE the Download after that time:
    // NOTE: IT will NOT be Resumed: This is here to test the behaviour of the Downloader Handler in PAUSED Mode
    //private static final Duration TIMEOUT_TO_PAUSE = Duration.ofSeconds(60)
    private static final Duration TIMEOUT_TO_PAUSE = null;

    // It creates a GET_HEADER Message using the block locator hash given (which is in human-readable format)
    private GetHeadersMsg buildGetHeadersMsg(ProtocolConfig protocolConfig, String blockHashHex) {

        List<HashMsg> hashMsgs = new ArrayList<>();
        HashMsg hashMsg = HashMsg.builder().hash(Utils.reverseBytes(Utils.HEX.decode(blockHashHex))).build();
        hashMsgs.add(hashMsg);

        BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg = BaseGetDataAndHeaderMsg.builder()
                .version(protocolConfig.getBasicConfig().getProtocolVersion())
                .blockLocatorHash(hashMsgs)
                .hashCount(VarIntMsg.builder().value(1).build())
                .hashStop(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                .build();

        GetHeadersMsg getHeadersMsg = GetHeadersMsg.builder()
                .baseGetDataAndHeaderMsg(baseGetDataAndHeaderMsg)
                .build();

        return getHeadersMsg;
    }

    @Ignore
    def "Test download Chain from hash Locator"() {
        given:

            // We Configure the P2P Connection:
            ProtocolConfig config = ProtocolConfigBuilder.get(Net.MAINNET.params())

            // Network Config:
            NetworkConfig networkConfig = new NetworkDefaultConfig().toBuilder()
                .maxSocketConnectionsOpeningAtSameTime(100)
                .timeoutSocketConnection(OptionalInt.of(100))       // 100 millisecs
                .timeoutSocketRemoteConfirmation(OptionalInt.of(1000))  // 1 sec
                .build()


            // Basic Config:
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                    .minPeers(OptionalInt.of(10))
                    .maxPeers(OptionalInt.of(20))
                    .build()

            // We set up the Download configuration:
            BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                    .maxBlocksInParallel(10)
                    .maxIdleTimeout(Duration.ofSeconds(10))
                    .maxMBinParallel(10_000)
                    .minSpeed(0)
                    .build()

            // We extends the DiscoveryHandler Config, in case DNS's are not working properly:
            DiscoveryHandlerConfig discoveryConfig = IntegrationUtils.getDiscoveryHandlerConfigMainnet(config.getDiscoveryConfig())

            // We configure the P2P Service:
            P2P p2p = new P2PBuilder("testing")
                    .config(networkConfig)
                    .config(config)
                    .config(basicConfig)
                    .config(blockConfig)
                    .config(discoveryConfig)
                    .publishState(BlockDownloaderHandler.HANDLER_ID, Duration.ofMillis(1000))
                    .publishState(NetworkHandler.HANDLER_ID, Duration.ofMillis(500))
                    .publishState(HandshakeHandler.HANDLER_ID, Duration.ofMillis(500))
                    .build()

            // A Lock, to keep our mind sane during the whole process:
            Lock lock = new ReentrantLock()

            // We are keeping track of the Blocks being downloaded:
            Set<String> blocksToDownload = java.util.concurrent.ConcurrentHashMap.newKeySet()
            Set<String> blocksDownloaded = java.util.concurrent.ConcurrentHashMap.newKeySet()                        // Blocks fully downloaded
            Set<String> blocksDiscarded = java.util.concurrent.ConcurrentHashMap.newKeySet()                          // Blocks Discarded...

            // We capture the state when we reach the min Peers, so we do not start the download until this moment:
            AtomicBoolean connReady = new AtomicBoolean(false);

            // We keep track of the the total size and the time spent:
            AtomicLong bytesDownloaded = new AtomicLong()
            AtomicInteger currentSpeedKbPerSec = new AtomicInteger()
            Instant startTime = null

            p2p.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({ e -> connReady.set(true) })

            // Every time we receive a HEADERS message from Remote Peers, we add the block Hashes there to the download list:
            p2p.EVENTS.MSGS.HEADERS.forEach({ e ->

                try {
                    lock.lock();
                    // We use the HEADERS receive to feed the list of blocks to download:

                    List<String> blocksToAdd = e.getBtcMsg().getBody().getBlockHeaderMsgList().stream()
                            .map({ header -> Utils.HEX.encode(header.getHash().getBytes()) })
                            .collect(Collectors.toList())

                    if (!blocksToAdd.isEmpty()) {
                        List<String> newBlocksToDownload = blocksToAdd.stream()
                                .filter({h -> !blocksToDownload.contains(h)})
                                .collect(Collectors.toList());
                        if (!newBlocksToDownload.isEmpty()) {

                            blocksToDownload.addAll(newBlocksToDownload)
                            p2p.REQUESTS.BLOCKS.download(newBlocksToDownload).submit()

                            // And we send out other GET_HEADERS Msg,, so we keep building the Chain:
                            String hashLocator = newBlocksToDownload.get(newBlocksToDownload.size() - 1);
                            p2p.REQUESTS.MSGS.broadcast(buildGetHeadersMsg(config, hashLocator)).submit()

                            println("> HEADERS Received, " + blocksToDownload.size() + " blocks requested.");
                        }
                    }
                } finally {
                    lock.unlock()
                }

            })

            // Blocks fully downloaded
            p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->

                    String blockHash = Utils.HEX.encode(e.getBlockHeader().hash.getBytes())
                    blocksDownloaded.add(blockHash)
                    bytesDownloaded.addAndGet(e.getBlockSize())
                    // Downloading speed (Kb/Sec) calculation:
                    long secsElapsed = Duration.between(startTime, Instant.now()).toSeconds()
                    if (secsElapsed > 0) {
                        currentSpeedKbPerSec.set((int) ((bytesDownloaded.get() / secsElapsed) / 1_000))
                    }
                    println(" > " + blockHash + " downloaded. " + blocksDownloaded.size() + " blocks. " + (bytesDownloaded.get() / 1_000_000) + " MB. " + currentSpeedKbPerSec + " Kbs/sec, " + blocksToDownload.size() + " blocks Requested in total")

            })

            // Blocks discarded
            p2p.EVENTS.BLOCKS.BLOCK_DISCARDED.forEach({ e ->
                blocksDiscarded.add(e.hash)
                println(" > Block " + e.hash + " discarded : " + e.reason)
            })

            // We log Status:
            p2p.EVENTS.STATE.BLOCKS.forEach( {e ->
                // We only print the detail block Download status if the Speed is strangley low (lower than 1MB/sec);
                println(Instant.now().toString() + " :: " + e)
            })
            p2p.EVENTS.STATE.HANDSHAKE.forEach({ e -> println(e)})
            p2p.EVENTS.STATE.NETWORK.forEach({ e -> println(e)})

        when:
            println(" > Testing Block Download in " + config.toString() + "...")
            // We start the Service and request to download the Blocks...
            p2p.start()

            // we wait until we reach the minimum number of Peers:
            while (!connReady.get()) Thread.sleep(100)
            println("Connection Ready...")

            Thread.sleep(1000)

            // After this moment, we can start downloading Blocks. The next Blocks to download will be injected after
            // we receive the HEADERS messages from the remote Peers....
            p2p.REQUESTS.MSGS.broadcast(buildGetHeadersMsg(config, HASH_LOCATOR)).submit()

            startTime = Instant.now()

            // We wait until we get some HEADERS in response, so we can feed our BlocksToDownload List:
            while (blocksToDownload.isEmpty()) Thread.sleep(1000)

            println("Blocks downloading Starts now...")

            // At this moment, the downloading has started.
            // We'll wait until all the blocks are done (downloaded or discarded) OR we've waited for too long...

            boolean allBlocksDone = false
            boolean timeoutBroken = false;
            boolean paused = false;
            while (!allBlocksDone && !timeoutBroken) {
                Thread.sleep(500)
                allBlocksDone = (blocksDownloaded.size() + blocksDiscarded.size()) == blocksToDownload.size()
                timeoutBroken = Duration.between(startTime, Instant.now()).compareTo(TIMEOUT) > 0

                // If specified, we PAUSE the Download process
                if ((TIMEOUT_TO_PAUSE != null) && !paused) {
                    if (Duration.between(startTime, Instant.now()).compareTo(TIMEOUT_TO_PAUSE) > 0) {
                        println("PAUSING DOWNLOAD...")
                        p2p.REQUESTS.BLOCKS.pause().submit();
                        paused = true;
                    }
                }

            }
            p2p.stop()
            Duration testDuration = Duration.between(startTime, Instant.now())
            println("It took " + testDuration.toSeconds() + " seconds to finish the Test.")
            if (timeoutBroken) println("Test Timeout BROKEN!")
        then:
            (blocksDownloaded.size() + blocksDiscarded.size()) == blocksToDownload.size()
    }
}
