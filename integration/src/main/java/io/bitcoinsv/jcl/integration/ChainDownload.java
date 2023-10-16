package io.bitcoinsv.jcl.integration;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.jcl.net.network.config.NetworkConfig;
import io.bitcoinsv.jcl.net.network.config.provided.NetworkDefaultConfig;
import io.bitcoinsv.jcl.net.network.handlers.NetworkHandler;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder;
import io.bitcoinsv.jcl.net.protocol.events.control.BlockDiscardedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.BlockDownloadedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.MinHandshakedPeersReachedEvent;
import io.bitcoinsv.jcl.net.protocol.events.data.HeadersMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandler;
import io.bitcoinsv.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.GetHeadersMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P;
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2023 nChain Ltd
 */
public class ChainDownload {

    private static Logger log = LoggerFactory.getLogger(ChainDownload.class);

    private ChainDownloadConfig downloadConfig;

    // Main JCL -NEt Object & config:
    private P2P p2p;

    // We keep track of the Downloading Process:
    AtomicBoolean connReady             = new AtomicBoolean(false);
    Set<String> blocksToDownload        = ConcurrentHashMap.newKeySet();
    Set<String> blocksDownloaded        = ConcurrentHashMap.newKeySet();
    Set<String> blocksDiscarded         = ConcurrentHashMap.newKeySet();
    Instant startTime                   = null;
    AtomicLong bytesDownloaded          = new AtomicLong();
    AtomicInteger currentSpeedKbPerSec  = new AtomicInteger();


    public ChainDownload() {}

    /*
     * It loads the Command line parameters and initializes
     */
    public void loadCmd(String ...args) {
        this.downloadConfig = new ChainDownloadConfig(args);
        this.p2p = setup(this.downloadConfig);
        this.registerEvents(p2p);
    }

    /*
     * It Starts the Downloading Process
     */
    public void start() throws Exception {
        log.info("Starting...");
        // We start the P2P Service...
        p2p.start();

        // we wait until we reach the minimum number of Peers:
        while (!connReady.get()) Thread.sleep(100);
        log.info("Connection Ready...");
        Thread.sleep(1000);

        // Now we can start downloading Blocks. We start requesting HEADERS for the First Block Download....
        p2p.REQUESTS.MSGS.broadcast(buildGetHeadersMsg(p2p.getProtocolConfig() , downloadConfig.getInitialBlock().toString())).submit();
        startTime = Instant.now();

        // We wait until we get some HEADERS in response, so we can feed our BlocksToDownload List:
        while (blocksToDownload.isEmpty()) Thread.sleep(1000);
        log.info("Blocks downloading Starts now...");
    }

    public void stop() throws Exception {
        p2p.stop();
    }

    /*
     * It prints the Help page for the commandline
     */
    private void printHelp() {
        log.info("---------------------------------------------------------------------------------");
        log.info("Usage: downloadChain.sh [NET] [HASH] [MIN_PEERS] [MAX_PEERS]");
        log.info(" [NET]       : Mandatory. Network to use: MAINNET, STN, etc (without quotes)");
        log.info(" [HASH]      : Mandatory. Hash of the First block to download. In HEX Format (without quotes)");
        log.info(" [MIN_PEERS] : Optional.  Minimum Number of Peers (default to {})", ChainDownloadConfig.DEF_MIN_PEERS);
        log.info(" [MAX_PEERS] : Optional.  Minimum Number of Peers (default to {})", ChainDownloadConfig.DEF_MAX_PEERS);
        log.info("---------------------------------------------------------------------------------");
    }

    /*
     * It creates a GET_HEADER Message using the block locator hash given (which is in human-readable format)
     */
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

    /*
     * It initializes the JCL-Net P2P Object based on the CMD parameters:
     */
    private P2P setup(ChainDownloadConfig downloadConfig) {

        // We Configure the P2P Connection:
        ProtocolConfig config = ProtocolConfigBuilder.get(downloadConfig.getNet().params());

        // Network Config:
        NetworkConfig networkConfig = new NetworkDefaultConfig().toBuilder()
                .maxSocketConnectionsOpeningAtSameTime(100)
                .timeoutSocketConnection(OptionalInt.of(100))           // 100 millisecs
                .timeoutSocketRemoteConfirmation(OptionalInt.of(1000))  // 1 sec
                .build();

        // Basic Config:
        ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(downloadConfig.getMinPeers()))
                .maxPeers(OptionalInt.of(downloadConfig.getMaxPeers()))
                .build();

        // We set up the Download configuration:
        BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(10)
                .maxIdleTimeout(Duration.ofSeconds(10))
                .maxMBinParallel(1_000)
                .minSpeed(0)
                .build();

        // We configure the P2P Service:
        P2P p2p = new P2PBuilder("testing")
                .config(networkConfig)
                .config(config)
                .config(basicConfig)
                .config(blockConfig)
                .publishState(BlockDownloaderHandler.HANDLER_ID, Duration.ofMillis(1000))
                .publishState(NetworkHandler.HANDLER_ID, Duration.ofMillis(500))
                .publishState(HandshakeHandler.HANDLER_ID, Duration.ofMillis(500))
                .build();

        return p2p;
    }

    // Event Handler...
    private void onMinPeersReached(MinHandshakedPeersReachedEvent e) {
        connReady.set(true);
    }

    // Event Handler...
    private synchronized void onHeadersMsgReceived(HeadersMsgReceivedEvent e) {
        List<String> blocksToAdd = e.getBtcMsg().getBody().getBlockHeaderMsgList().stream()
                .map( header -> Utils.HEX.encode(header.getHash().getBytes()))
                .collect(Collectors.toList());

        if (!blocksToAdd.isEmpty()) {
            // WE check if there are New Blocks to Download:
            List<String> newBlocksToDownload = blocksToAdd.stream()
                    .filter(h -> !blocksToDownload.contains(h))
                    .collect(Collectors.toList());

            if (!newBlocksToDownload.isEmpty()) {
                // We add the new Blocks to the Download List:
                blocksToDownload.addAll(newBlocksToDownload);
                p2p.REQUESTS.BLOCKS.download(newBlocksToDownload).submit();

                // And we send out other GET_HEADERS Msg, so we keep building the Chain:
                String hashLocator = newBlocksToDownload.get(newBlocksToDownload.size() - 1);
                p2p.REQUESTS.MSGS.broadcast(buildGetHeadersMsg(p2p.getProtocolConfig(), hashLocator)).submit();
                log.info("> HEADERS Received, {} more Blocks requested.",blocksToDownload.size());
            }
        }
    }

    // Event Handler...
    private synchronized void onWholeBlockDownload(BlockDownloadedEvent e) {
        String blockHash = Utils.HEX.encode(e.getBlockHeader().getHash().getBytes());
        blocksDownloaded.add(blockHash);
        bytesDownloaded.addAndGet(e.getBlockSize());
        // Downloading speed (Kb/Sec) calculation:
        long secsElapsed = Duration.between(startTime, Instant.now()).toSeconds();
        if (secsElapsed > 0) {
            currentSpeedKbPerSec.set((int) ((bytesDownloaded.get() / secsElapsed) / 1_000));
        }
        log.info("> BLOCK {}  downloaded. Summary: {} blocks, {} MB, {} Kbs/sec, {} blocks Requested",
                blockHash,
                blocksDownloaded.size(),
                (bytesDownloaded.get() / 1_000_000),
                currentSpeedKbPerSec,
                blocksToDownload.size());
    }

    // Event Handler...
    private synchronized void onBlockDiscarded(BlockDiscardedEvent e) {
        blocksDiscarded.add(e.getHash());
        log.info(" > Block {} discarded: {}", e.getHash(), e.getReason());
    }

    /*
     * It registers P2P Events so we can react to the Network activity
     */
    private void registerEvents(P2P p2p) {
        p2p.EVENTS.STATE.BLOCKS.forEach( e -> log.info(e.toString()));
        p2p.EVENTS.STATE.HANDSHAKE.forEach(e -> log.info(e.toString()));
        p2p.EVENTS.STATE.NETWORK.forEach(e -> log.info(e.toString()));

        p2p.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach(this::onMinPeersReached);
        p2p.EVENTS.MSGS.HEADERS.forEach(this::onHeadersMsgReceived);
        p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach(this::onWholeBlockDownload);
        p2p.EVENTS.BLOCKS.BLOCK_DISCARDED.forEach(this::onBlockDiscarded);
    }

    public static void main(String ...args) {
        try {
            ChainDownload chainDownload = new ChainDownload();
            if (args.length <= 1) {
                chainDownload.printHelp();
            } else {
                chainDownload.loadCmd(args);
                chainDownload.start();
                // We just keep going until the process is killed:
                while (true) Thread.sleep(10_000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
