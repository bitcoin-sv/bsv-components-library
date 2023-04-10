package io.bitcoinsv.bsvcl.net.integration.protocol.wrapper.receivers

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bsvcl.tools.bigObjects.BigCollectionChunk
import io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events.BigObjectReceivedEvent
import io.bitcoinsv.bsvcl.tools.config.RuntimeConfig
import io.bitcoinsv.bsvcl.tools.config.provided.RuntimeConfigDefault
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

class RawBlockReceiver_ChainDownloadTest extends Specification {

    // We start downloading the Chain AFTER this Block:
    private static final String HASH_LOCATOR = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"; // Block #0
    //private static final String HASH_LOCATOR = "00000000000000000102d94fde9bd0807a2cc7582fe85dd6349b73ce4e8d9322"; // BSV Fork

    // Connection params:
    private static final int MIN_PEERS = 10
    private static final int MAX_PEERS = 15
    private static final int MAX_BLOCKS_IN_PARALLEL = 5

    // Maximum Test Duration
    private static final Duration TEST_TIME_LIMIT = Duration.ofMinutes(10);

    // Net work Configuration, P2P & BLOCK RECEIVER
    private io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p;
    private io.bitcoinsv.bsvcl.net.protocol.wrapper.receivers.RawBlockReceiver receiver

    // Multi-Threads mental health:
    Lock lock = new ReentrantLock()

    // Variables to Keep track of the State:
    // ==============================================================================================

    private AtomicBoolean   trackConnReady            = new AtomicBoolean(false);
    private AtomicLong      trackBytesDownloaded      = new AtomicLong()
    private AtomicInteger   trackCurrentSpeedKbPerSec = new AtomicInteger()
    private List<String>    trackBlocksToDownload     = new ArrayList<>()


    /** It creates a GET_HEADER Message using the block locator hash given (which is in human-readable format) */
    private io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg buildGetHeadersMsg(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig, String blockHashHex) {

        List<io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg> hashMsgs = new ArrayList<>();
        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hashMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(Utils.reverseBytes(Utils.HEX.decode(blockHashHex))).build();
        hashMsgs.add(hashMsg);

        io.bitcoinsv.bsvcl.net.protocol.messages.BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg = io.bitcoinsv.bsvcl.net.protocol.messages.BaseGetDataAndHeaderMsg.builder()
                .version(protocolConfig.getBasicConfig().getProtocolVersion())
                .blockLocatorHash(hashMsgs)
                .hashCount(io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(1).build())
                .hashStop(io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                .build();

        io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg getHeadersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersMsg.builder()
                .baseGetDataAndHeaderMsg(baseGetDataAndHeaderMsg)
                .build();

        return getHeadersMsg;
    }

    /**
     * Runs when a HEADER msg is received. We rquest the blocks announced for download adn broadcast a new GET_HEADERS
     * so we keep building the Chain...
     */
    private void onHeadersMsgReceived(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig, io.bitcoinsv.bsvcl.net.protocol.events.data.HeadersMsgReceivedEvent event) {
        try {
            lock.lock()
            // We use the HEADERS receive to feed the list of blocks to download:
            List<String> blocksToAdd = event.getBtcMsg().getBody().getBlockHeaderMsgList().stream()
                    .map({ header -> Utils.HEX.encode(header.getHash().getBytes()) })
                    .filter({hash -> !trackBlocksToDownload.contains(hash)})
                    .collect(Collectors.toList())
            trackBlocksToDownload.addAll(blocksToAdd)

            if (blocksToAdd.size() > 0) {
                // We request those blocks to Download:
                p2p.REQUESTS.BLOCKS.download(blocksToAdd).submit()
                println(">> REQUESTING " + blocksToAdd.size() + " new Blocks to Download...")

                // We broadcast a new GET_HEADERS Msg...
                String hashLocator = blocksToAdd.get(blocksToAdd.size() - 1)
                io.bitcoinsv.bsvcl.net.protocol.messages.common.Message getHeadersMsg = buildGetHeadersMsg(protocolConfig, hashLocator); // locator is the LAST Block
                p2p.REQUESTS.MSGS.broadcast(getHeadersMsg).submit()
                println(">> HEADERS Received from " + event.peerAddress.toString() + ", BROADCASTING A GET_HEADERS, LOCATOR = " + hashLocator + "...")
            }

        } finally {
            lock.unlock()
        }
    }

    /** Runs when a Block has been completely received. We print some info about it **/
    private void onBlockDownloaded(BigObjectReceivedEvent event) {
        // We get an iterator and then we remove the whole Block:
        Iterator<BigCollectionChunk<io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg>> chunksIt = receiver.getTxs(event.getObjectId());
        long txBytes = 0;
        long numTxs;
        while (chunksIt.hasNext()) {
            BigCollectionChunk<io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg> chunk = chunksIt.next()
            txBytes += chunk.items.stream().mapToLong({i -> i.lengthInBytes}).sum()
            numTxs += chunk.items.size();
        }
        trackBytesDownloaded.addAndGet(txBytes)
        receiver.remove(event.getObjectId())
        //println(">>>>>> BLOCK #" + event.getObjectId() + " :: downloaded from " + event.getSource() + " :: " + numTxs + " Txs [" + txBytes + " bytes].")
    }

    /* Utility function */
    private String formatSize(long sizeInBytes) {
        String result = (sizeInBytes < 1000)
                ? (sizeInBytes + " bytes")
                : (sizeInBytes < 1_000_000)
                ? ((sizeInBytes / 1000) + " KBs")
                : ((sizeInBytes / 1_000_000) + " MBs");
        return result;
    }

    private void printStatus() {
        println(">> DATA DOWNLOADED (Txs): " + formatSize(trackBytesDownloaded.get()))
    }

    @Ignore
    def "testing Download Chain"() {
        given:
        // Network Configuration
        // ==============================================================================================

        // P2P Service Configuration:
        // ----------------------------------------------------------------------------------------------

        // Network protocol:
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig = new io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig();

        // Runtime Config:
        RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                .msgSizeInBytesForRealTimeProcessing(5_000_000)
                .build()

        // Basic Config:
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(MIN_PEERS))
                .maxPeers(OptionalInt.of(MAX_PEERS))
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                .build()

        // Serialization Config:
        io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig messageConfig = protocolConfig.getMessageConfig().toBuilder()
                .rawTxsEnabled(true)
                .build();

        // We set up the Download configuration:
        io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig blockConfig = protocolConfig.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(MAX_BLOCKS_IN_PARALLEL)
                .removeBlockHistoryAfterDownload(false)
                .build()

        // We extends the DiscoveryHandler Config, in case DNS's are not working properly:
        io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig discoveryConfig = io.bitcoinsv.bsvcl.net.integration.utils.IntegrationUtils.getDiscoveryHandlerConfigMainnet(config.getDiscoveryConfig())

        // We configure the P2P Service:
        p2p = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("testing")
                .config(runtimeConfig)
                .config(protocolConfig)
                .config(basicConfig)
                .config(messageConfig)
                .config(blockConfig)
                .config(discoveryConfig)
                .publishStates(Duration.ofMillis(500))
                .build()

        // P2P Events:
        // ----------------------------------------------------------------------------------------------

        // When we reach MAX_PEERS, we activate the FLAG:
        p2p.EVENTS.PEERS.HANDSHAKED_MAX_REACHED.forEach({ e -> trackConnReady.set(true) })

        // When we get a HEADERS, we request those Blocks to download and we broadcast another GET_HEADERS Message,
        // to keep going up the chain...
        p2p.EVENTS.MSGS.HEADERS.forEach({ e -> onHeadersMsgReceived(protocolConfig, e)})

        // We print the DOWNLOADER Handler State (after reaching MAX_PEERS):
        p2p.EVENTS.STATE.BLOCKS.forEach({e ->
            if (trackConnReady.get()) {
                println(e)
                printStatus()
            }
        })

        // Other states are logged while we haven't reached MAX_PEERS yet...
        p2p.EVENTS.STATE.HANDSHAKE.forEach({e -> if (!trackConnReady.get()) println(e)})

        // RAW BLOCK RECEIVER Configuration
        // ==============================================================================================

        receiver = new io.bitcoinsv.bsvcl.net.protocol.wrapper.receivers.RawBlockReceiver("testingRawBlockReceiver_chainDownload", p2p)
        receiver.EVENTS.OBJECT_RECEIVED(10).forEach({ e -> this.onBlockDownloaded(e)})

        when:

        // WE start the Test:
        receiver.start()
        p2p.start()
        Instant timestampBegin = Instant.now();

        // We wait until we reach MAX_PEERS:
        while (!trackConnReady.get()) {Thread.sleep(1000)}

        // WE broadcast a GET_HEADERS using the HASH_LOCATOR Defined so we trigger the whole process:
        p2p.REQUESTS.MSGS.broadcast(buildGetHeadersMsg(protocolConfig, HASH_LOCATOR)).submit()
        println(">> BROADCASTING A GET_HEADERS, LOCATOR = " + HASH_LOCATOR + "...")

        // We wait until the TIME_LIMIT has expired
        while (Duration.between(timestampBegin, Instant.now()).compareTo(TEST_TIME_LIMIT) < 0) { Thread.sleep(1000)}

        p2p.stop()
        receiver.destroy()
        then:
        true
    }
}