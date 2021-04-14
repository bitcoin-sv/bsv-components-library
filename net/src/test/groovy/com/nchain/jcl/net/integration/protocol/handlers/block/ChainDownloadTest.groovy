package com.nchain.jcl.net.integration.protocol.handlers.block

import com.nchain.jcl.net.network.config.NetworkConfig
import com.nchain.jcl.net.network.config.provided.NetworkDefaultConfig
import com.nchain.jcl.net.network.handlers.NetworkHandler
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandler
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerState
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandler
import com.nchain.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.net.protocol.messages.GetHeadersMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.VarIntMsg
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors

/**
 * A testing class for downloading a partial part of the Blockchain, including both Blocks and Txs.
 */
class ChainDownloadTest extends Specification {

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

    def "Test download Chain from hash Locator"() {
        given:
            // The longest Timeout we'll wait for to run the test:
            Duration TIMEOUT = Duration.ofMinutes(15)

            // We'll download all the blocks mined AFTER this block:
            String HASH_LOCATOR = "000000000000000002f5268d72f9c79f29bef494e350e58f624bcf28700a1846"

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
                    .minPeers(OptionalInt.of(15))
                    .maxPeers(OptionalInt.of(20))
                    .build()

            // We set up the Download configuration:
            BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                    .maxBlocksInParallel(15)
                    .maxIdleTimeout(Duration.ofSeconds(10))
                    .build()

            // We configure the P2P Service:
            P2P p2p = new P2PBuilder("testing")
                    .config(networkConfig)
                    .config(config)
                    .config(basicConfig)
                    .config(blockConfig)
                    .publishState(BlockDownloaderHandler.HANDLER_ID, Duration.ofMillis(500))
                   // .publishState(NetworkHandler.HANDLER_ID, Duration.ofMillis(500))
                    .publishState(HandshakeHandler.HANDLER_ID, Duration.ofMillis(500))
                    .build()

            // A Lock, to keep our mind sane during the whole process:
            Lock lock = new ReentrantLock()

            // We are keeping track of the Blocks being downloaded:
            Set<String> blocksToDownload = new HashSet<>()
            Set<String> blocksDownloaded = new HashSet<>()                          // Blocks fully downloaded
            Set<String> blocksDiscarded = new HashSet<>()                           // Blocks Discarded...

            // We capture the state when we reach the min Peers, so we do not start the download until this moment:
            AtomicBoolean connReady = new AtomicBoolean(false);

            // We keep track of the the total size and the time spent:
            AtomicLong bytesDownloaded = new AtomicLong()
            AtomicInteger currentSpeedKbPerSec = new AtomicInteger()
            Instant startTime = null

            p2p.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({ e -> connReady.set(true) })

            // Every time we receive a HEADERS message from Remote Peers, we add the block Hashes there to the
            // download list:
            p2p.EVENTS.MSGS.HEADERS.forEach({ e ->
                // We use the HEADERS receive to feed the list of blocks to download:
                List<String> blocksToAdd = e.getBtcMsg().getBody().getBlockHeaderMsgList().stream()
                        .map({ header -> Utils.HEX.encode(Utils.reverseBytes(header.getHash().getHashBytes())) })
                        .collect(Collectors.toList())

                blocksToDownload.addAll(blocksToAdd)
                p2p.REQUESTS.BLOCKS.download(blocksToAdd).submit()

            })

            // Blocks fully downloaded
            p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->
                try {
                    lock.lock()
                    String blockHash = Utils.HEX.encode(Utils.reverseBytes(e.getBlockHeader().hash.getHashBytes()))
                    blocksDownloaded.add(blockHash)
                    bytesDownloaded.addAndGet(e.getBlockSize())
                    // Downloading speed (Kb/Sec) calculation:
                    long secsElapsed = Duration.between(startTime, Instant.now()).toSeconds()
                    if (secsElapsed > 0) {
                        currentSpeedKbPerSec.set((int) ((bytesDownloaded.get() / secsElapsed) / 1_000))
                    }
                    println(" > " + blockHash + " downloaded. " + blocksDownloaded.size() + " blocks. " + (bytesDownloaded.get() / 1_000_000) + " MB. " + currentSpeedKbPerSec + " Kbs/sec")
                } finally {
                    lock.unlock()
                }
            })

            // Blocks discarded
            p2p.EVENTS.BLOCKS.BLOCK_DISCARDED.forEach({ e ->
                blocksDiscarded.add(e.hash)
                println(" > Block " + e.hash + " discarded : " + e.reason)
            })

            // We log Status:
            p2p.EVENTS.STATE.BLOCKS.forEach( {e ->
                // We only print the detail block Download status if the Speed is strangley low (lower than 1MB/sec);
                BlockDownloaderHandlerState state = (BlockDownloaderHandlerState) e.getState();
                if (currentSpeedKbPerSec.get() < 1000) {
                    println(e)
                } else {
                    println("Peers: " +
                            state.getPeersInfo().size() +
                            " total, " + state.getNumPeersDownloading() + " downloading. " +
                            "Blocks: " + state.getDownloadedBlocks().size() + " downloaded, " +
                            state.getDiscardedBlocks().size() + " discarded, " + state.getPendingBlocks().size() + " pending")
                }
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

            // After this moment, we can start downloading Blocks. The next Blocks to download will be injected after
            // we receive the HEADERS messages from the remote Peers....
            p2p.REQUESTS.MSGS.broadcast(buildGetHeadersMsg(config, HASH_LOCATOR)).submit()

            startTime = Instant.now()

            // We wait until we get some HEADERS in response, so we can feed ur BlocksToDownload List:
            while (blocksToDownload.isEmpty()) Thread.sleep(1000)
            println("Blocks downloading Starts now...")

            // At this moment, the downloading has started.
            // We'll wait until all the blocks are done (downloaded or discarded) OR we've waited for too long...

            boolean allBlocksDone = false
            boolean timeoutBroken = false;
            while (!allBlocksDone && !timeoutBroken) {
                Thread.sleep(500)
                allBlocksDone = (blocksDownloaded.size() + blocksDiscarded.size()) == blocksToDownload.size()
                timeoutBroken = Duration.between(startTime, Instant.now()).compareTo(TIMEOUT) > 0
            }
            p2p.stop()
            Duration testDuration = Duration.between(startTime, Instant.now())
            println("It took " + testDuration.toSeconds() + " seconds to finish the Test.")
            if (timeoutBroken) println("Test Timeout BROKEN!")
        then:
            (blocksDownloaded.size() + blocksDiscarded.size()) == blocksToDownload.size()
    }
}
