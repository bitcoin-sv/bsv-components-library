package com.nchain.jcl.integration;

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.config.ProtocolConfig;
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder;
import com.nchain.jcl.net.protocol.events.control.PeerHandshakedDisconnectedEvent;
import com.nchain.jcl.net.protocol.events.control.PeerHandshakedEvent;
import com.nchain.jcl.net.protocol.events.data.InvMsgReceivedEvent;
import com.nchain.jcl.net.protocol.events.data.RawTxMsgReceivedEvent;
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandler;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandler;
import com.nchain.jcl.net.protocol.messages.GetdataMsg;
import com.nchain.jcl.net.protocol.messages.InventoryVectorMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import com.nchain.jcl.net.protocol.wrapper.P2P;
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder;
import com.nchain.jcl.tools.events.EventQueueProcessor;
import com.nchain.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.core.Utils;
import io.bitcoinj.params.Net;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.params.RegTestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 16/07/2021
 */
public class JCLServer {

    private final Logger log = LoggerFactory.getLogger(JCLServer.class);

    // PARAMETERS:
    private final Integer MIN_PEERS = 1;
    private final Integer MAX_PEERS = 1;
    private final NetworkParameters NETWORK_PARAMS = new RegTestParams(Net.REGTEST);

    // Time when we receive the FIRST and LAST TXs:
    Instant firstTxInstant = null;
    Instant lastTxInstant = null;

    // Counters:
    AtomicInteger numPeersHandshaked = new AtomicInteger();
    AtomicLong numTxs = new AtomicLong();
    AtomicLong numINVs = new AtomicLong();

    // Logging Thread:
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    // JCL P2P Service:
    private P2P p2p;

    public JCLServer() {

        // We configure the P2P connection:
        ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(NETWORK_PARAMS);

         // Protocol Basic Configuration: We set up the Range of Peers:
        ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(MIN_PEERS))
                .maxPeers(OptionalInt.of(MAX_PEERS))
                .build();

        // We enable the Tx Relay:
        HandshakeHandlerConfig handshakeConfig = protocolConfig.getHandshakeConfig().toBuilder()
                .relayTxs(true)
                .build();

        MessageHandlerConfig messageConfig = protocolConfig.getMessageConfig().toBuilder()
                .rawTxsEnabled(true) // IMPORTANT: It affects both Tx and Blocks
                .build();

        // We build the P2P Service
        this.p2p = new P2PBuilder("JCLServer")
                .config(protocolConfig)
                .config(basicConfig)
                .config(messageConfig)
                .config(handshakeConfig)
                .publishStates(Duration.ofSeconds(5))       // we publish all Handler states
                .excludeHandler(PingPongHandler.HANDLER_ID)
                .excludeHandler(BlockDownloaderHandler.HANDLER_ID)
                .build();

        p2p.EVENTS.PEERS.HANDSHAKED.forEach(this::onPeerHandshaked);
        p2p.EVENTS.PEERS.HANDSHAKED_DISCONNECTED.forEach(this::onPeerDisconnected);
        p2p.EVENTS.MSGS.INV.forEach(this::processINV);
        p2p.EVENTS.MSGS.TX_RAW.forEach(this::processRawTX);

    }

    public void start() {
        p2p.startServer();
        executor.scheduleAtFixedRate(this::log, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        p2p.stop();
        executor.shutdownNow();
    }

    private void onPeerHandshaked(PeerHandshakedEvent event) {
        numPeersHandshaked.incrementAndGet();
        log.info("JCL Server :: Peer Connected: " + event.getPeerAddress() + " : " + event.getVersionMsg().getUser_agent().getStr());
    }

    private void onPeerDisconnected(PeerHandshakedDisconnectedEvent event) {
        numPeersHandshaked.decrementAndGet();
        log.info("JCL Server :: Peer Disconnected: " + event.getPeerAddress() + " : " + event.getVersionMsg().getUser_agent().getStr());
    }

    private void processINV(InvMsgReceivedEvent event) {
        List<InventoryVectorMsg> newTxsInvItems =  event.getBtcMsg().getBody().getInvVectorList();
        // WE only process the INV after reaching the MINIMUM set of Peers:
        if (MIN_PEERS == null || numPeersHandshaked.get() >= MIN_PEERS) {

            if (newTxsInvItems.size() > 0) {
                // Now we send a GetData asking for them....
                GetdataMsg getDataMsg = GetdataMsg.builder().invVectorList(newTxsInvItems).build();
                BitcoinMsg<GetdataMsg> btcGetDataMsg = new BitcoinMsgBuilder(p2p.getProtocolConfig().getBasicConfig(), getDataMsg).build();
                p2p.REQUESTS.MSGS.send(event.getPeerAddress(), btcGetDataMsg).submit();
            }
        }
    }

    private void processRawTX(RawTxMsgReceivedEvent event) {
        if (firstTxInstant == null)
            firstTxInstant = Instant.now();

        lastTxInstant = Instant.now();
        numTxs.incrementAndGet();
    }

    private void log() {
        // Performance log:
        log.info("JCL Server :: Performance : " +  numPeersHandshaked + " peers, " + numTxs.get() + " Txs received.");

        // Threads log:
        log.info("JCL Server :: Threads     : " + ThreadUtils.getThreadsInfo());

        // Memory log:
        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMem = Runtime.getRuntime().maxMemory() - usedMem;
        String memorySummary = String.format("totalMem: %s, maxMem: %s, freeMem: %s, usedMem: %s, availableMem: %s",
                Utils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), false),
                Utils.humanReadableByteCount(Runtime.getRuntime().maxMemory(), false),
                Utils.humanReadableByteCount(Runtime.getRuntime().freeMemory(), false),
                Utils.humanReadableByteCount(usedMem, false),
                Utils.humanReadableByteCount(availableMem, false));
        log.info("JCL Server :: Memory      : " + memorySummary);
    }

    public static void main(String args[]) {

        JCLServer server = new JCLServer();
        server.start();
        // For now, we do NOT stop...
    }
}