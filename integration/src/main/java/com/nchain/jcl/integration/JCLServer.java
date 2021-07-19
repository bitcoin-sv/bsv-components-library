package com.nchain.jcl.integration;

import com.nchain.jcl.net.network.handlers.NetworkHandlerState;
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
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerState;
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
import io.bitcoinj.params.STNParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author m.fletcher@nchain.com
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 16/07/2021
 */
public class JCLServer {

    private final Logger log = LoggerFactory.getLogger(JCLServer.class);

    // BASIC PARAMETERS:
    private final Integer MIN_PEERS = 1;
    private final Integer MAX_PEERS = 1;
    //private String NET = Net.STN.name();
    private String NET = Net.REGTEST.name();

    // List of initial Nodes to connect to, specific for some Networks:
    String[] STN_INITIAL_PEERS = new String[]{
            // From Brad:
            "209.97.128.49:9333",   "188.166.44.242:9333",  "165.22.58.146:9333",
            "206.189.42.110:9333",  "165.22.59.150:9333",   "116.202.171.166:9333",
            "95.217.38.94:9333",    "116.202.113.92:9333",  "116.202.118.183:9333",
            "46.4.76.249:9333",     "95.217.121.173:9333",   "116.202.234.249:9333",
            "95.217.108.109:9333",
            // From Esthon:
            "104.154.79.59:9333",   "35.184.152.150:9333",  "35.188.22.213:9333",
            "35.224.150.17:9333",   "104.197.96.163:9333",  "34.68.205.136:9333",
            "34.70.95.165:9333",    "34.70.152.148:9333",   "104.154.79.59:9333",
            "35.232.247.207:9333",
            // From WhatOnChain.com:
            "37.122.249.164:9333",  "95.217.121.173:9333",  "165.22.58.146:9333",
            "46.4.76.249:9333",     "134.122.102.58:9333",  "178.128.169.224:9333",
            "206.189.42.110:9333",  "116.202.171.166:9333", "178.62.11.170:9333",
            "34.70.152.148:9333",   "95.217.121.173:9333",  "116.202.118.183:9333",
            "139.59.78.14:9333",    "37.122.249.164:9333",  "165.22.127.22:9333",
            "78.110.160.26:9333",   "209.97.181.106:9333",  "64.227.40.244:9333",
            "35.184.152.150:9333",  "212.89.6.129:9333"
    };

    // List of Initial Peers to Connect on startup, broken down in diffferent Networks:
    private Map<String, String[]> INITIAL_PEERS = new HashMap<>() {{ put(Net.STN.name(), STN_INITIAL_PEERS);}};

    // Time when we receive the FIRST and LAST TXs:
    Instant firstTxInstant = null;
    Instant lastTxInstant = null;

    // Counters:
    AtomicInteger numPeersHandshaked = new AtomicInteger();
    AtomicLong numTxs = new AtomicLong();
    AtomicLong numINVs = new AtomicLong();
    AtomicLong sizeTxs = new AtomicLong();

    // Logging:
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    AtomicInteger numTxsLog = new AtomicInteger();  // Number of Txs received between 2 logs:
    AtomicLong sizeTxsLog = new AtomicLong();       // Size of Txs received since last Log:
    Instant lastLogInstant = Instant.now();         // Last log timestamp:

    // JCL P2P Service:
    private P2P p2p;

    public JCLServer() {

        // We configure the P2P connection:
        NetworkParameters NETWORK_PARAMS = Net.valueOf(NET).params();
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
        P2PBuilder p2pBuilder = new P2PBuilder("JCLServer")
                .config(protocolConfig)
                .config(basicConfig)
                .config(messageConfig)
                .config(handshakeConfig)
                .publishStates(Duration.ofSeconds(5))       // we publish all Handler states
                .excludeHandler(BlockDownloaderHandler.HANDLER_ID);

        // if the Network is REGTEST we disable the PingPong Handler...
        if (NETWORK_PARAMS.getClass().equals(RegTestParams.class)) {
            p2pBuilder.excludeHandler(PingPongHandler.HANDLER_ID);
        }

        this.p2p = p2pBuilder.build();

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

    public void connectToInitialPeers() {
        if (INITIAL_PEERS.containsKey(NET)) {
            for (String initialPeer : INITIAL_PEERS.get(NET)) {
                p2p.REQUESTS.PEERS.connect(initialPeer).submit();
            }
        }
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
        numINVs.incrementAndGet();

        if (newTxsInvItems.size() > 0) {
            // Now we send a GetData asking for them....
            GetdataMsg getDataMsg = GetdataMsg.builder().invVectorList(newTxsInvItems).build();
            BitcoinMsg<GetdataMsg> btcGetDataMsg = new BitcoinMsgBuilder(p2p.getProtocolConfig().getBasicConfig(), getDataMsg).build();
            p2p.REQUESTS.MSGS.send(event.getPeerAddress(), btcGetDataMsg).submit();
        }
    }

    private void processRawTX(RawTxMsgReceivedEvent event) {
        if (firstTxInstant == null)
            firstTxInstant = Instant.now();

        lastTxInstant = Instant.now();
        numTxs.incrementAndGet();
        sizeTxs.addAndGet(event.getBtcMsg().getBody().getContent().length);
        numTxsLog.incrementAndGet();
        sizeTxsLog.addAndGet(event.getBtcMsg().getBody().getContent().length);
    }

    private void log() {
        // Performance log:
        StringBuffer logLine = new StringBuffer("JCL Server :: Performance : " + numPeersHandshaked + " peers");
        if (firstTxInstant != null) {
            int txsPerSec = (int) (((double) numTxsLog.get() / (Duration.between(lastLogInstant, Instant.now()).toMillis())) * 1000);
            logLine.append(", " + numTxs.get() + " Txs received, " + txsPerSec + " txs/sec");
            // We format the accumulated Txs Size:
            String txsSize = (sizeTxsLog.get() < 1000)
                    ? sizeTxsLog.get() + " bytes"
                    : (sizeTxsLog.get() < 1_000_000)
                        ? (sizeTxsLog.get() / 1000) + " KB"
                        : (sizeTxsLog.get()/1_000_000) + " MB";
            logLine.append(", " + txsSize + ", Total: " + sizeTxs);

            // reset:
            lastLogInstant = Instant.now();
            numTxsLog.set(0);
            sizeTxsLog.set(0);
        }
        log.info(logLine.toString());

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
        server.connectToInitialPeers();
        // For now, we do NOT stop...
    }
}