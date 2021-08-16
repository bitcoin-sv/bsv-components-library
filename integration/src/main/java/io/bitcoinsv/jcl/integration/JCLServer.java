/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.integration;

import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder;
import io.bitcoinsv.jcl.net.protocol.events.control.PeerHandshakedDisconnectedEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.PeerHandshakedEvent;
import io.bitcoinsv.jcl.net.protocol.events.data.InvMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.events.data.RawTxMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandler;
import io.bitcoinsv.jcl.net.protocol.messages.GetdataMsg;
import io.bitcoinsv.jcl.net.protocol.messages.InventoryVectorMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P;
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder;
import io.bitcoinsv.jcl.tools.config.RuntimeConfigImpl;
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.core.Utils;
import io.bitcoinj.params.Net;
import io.bitcoinj.params.NetworkParameters;
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

    private static final Logger log = LoggerFactory.getLogger(JCLServer.class);

    /** Configuration class for JCL Server that determines the Network to use and other parameters */
    static class JCLServerConfig {
        Net net;
        int minPeers;
        int maxPeers;
        boolean pingEnabled;
        Duration timeLimit;
        Integer maxThreads;
        boolean useCachedThreadPool;

        public JCLServerConfig(Net net, int minPeers, int maxPeers, boolean pingEnabled) {
            this.net = net;
            this.minPeers = minPeers;
            this.maxPeers = maxPeers;
            this.pingEnabled = pingEnabled;
            this.timeLimit = timeLimit;
        }
    }

    // Pre-Configured Configurations for MAINNET, STN, REGTEST:
    private static Map<Net, JCLServerConfig> CONFIGS = new HashMap<>(){
        {
            put(Net.MAINNET, new JCLServerConfig(Net.MAINNET, 10, 30, true));
            put(Net.STN, new JCLServerConfig(Net.STN, 10, 30, true));
            put(Net.REGTEST, new JCLServerConfig(Net.REGTEST, 1, 1, false));
        }};


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

    // Server Configuration:
    JCLServerConfig config;

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

    public JCLServer(JCLServerConfig config) {

        this.config = config;

        // We configure the P2P connection:
        NetworkParameters NETWORK_PARAMS = config.net.params();

        // Runtime Configuration:
        RuntimeConfigImpl runtimeConfig = new RuntimeConfigDefault();
        if (config.maxThreads != null) {
            runtimeConfig = runtimeConfig.toBuilder()
                    .maxNumThreadsForP2P(config.maxThreads)
                    .build();
        }

        if (config.useCachedThreadPool) {
            runtimeConfig = runtimeConfig.toBuilder()
                    .useCachedThreadPoolForP2P(true)
                    .build();
        }

        // Protocol Configuration:
        ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(NETWORK_PARAMS);

        // Protocol Basic Configuration: We set up the Range of Peers:
        ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(config.minPeers))
                .maxPeers(OptionalInt.of(config.maxPeers))
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
                .config(runtimeConfig)
                .config(protocolConfig)
                .config(basicConfig)
                .config(messageConfig)
                .config(handshakeConfig)
                .publishStates(Duration.ofSeconds(5))       // we publish all Handler states
                .excludeHandler(BlockDownloaderHandler.HANDLER_ID);

        // if the Network is REGTEST we disable the PingPong Handler...
        if (!config.pingEnabled) {
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
        log.info("JCL Server Started.");
        executor.scheduleAtFixedRate(this::log, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        p2p.stop();
        executor.shutdownNow();
        log.info("JCL Server Stopped.");
        printStatistics();
    }

    public void connectToInitialPeers() {
        if (INITIAL_PEERS.containsKey(config.net)) {
            for (String initialPeer : INITIAL_PEERS.get(config.net)) {
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

    private static String getParamValue(String paramName, String ...params) {
        String result = null;
        for (String param : params) {
            if (param.toUpperCase().indexOf(paramName.toUpperCase()) >= 0) {
                result = param.substring(param.indexOf("=") + 1);
            }
        }
        return result;
    }

    public static JCLServerConfig getConfigFromArguments(String ...args) {
        if (args.length < 1) { return null; }

        // We get the 'Net' parameter:
        String netValue = getParamValue("net", args);
        if (netValue == null) { return null; }
        Net net = Net.valueOf(netValue.toUpperCase());

        // We get the 'timeLimit' parameter;
        String timeLimitValue = getParamValue("timeLimit", args);
        Duration timeLimit = (timeLimitValue != null) ? Duration.ofSeconds(Integer.valueOf(timeLimitValue)) : null;

        // We get the 'maxThreads' parameter:
        String maxThreadsValue = getParamValue("maxThreads", args);
        Integer maxThreads = (maxThreadsValue != null) ? Integer.valueOf(maxThreadsValue) : null;

        // We get the 'useCachedPool' parameter:
        String useCachedThreadPoolStr = getParamValue("useCachedPool", args);
        boolean useCachedThreadPool = (useCachedThreadPoolStr != null) ? Boolean.valueOf(useCachedThreadPoolStr) : false;

        JCLServerConfig result = CONFIGS.get(net);
        result.timeLimit = timeLimit;
        result.maxThreads = maxThreads;
        result.useCachedThreadPool = useCachedThreadPool;
        return result;
    }

    public static void printHelp() {
        System.out.println("\n JCL Server Usage: java -jar jclServer.jar [net=XXX] (timeLimit=XXX) (maxThreads=XXX)");
        System.out.println(" - [net]       : Mandatory : Possible Values: mainnet, stn, regtest");
        System.out.println(" - (timeLimit) : Optional  : Time limit in seconds After that the Server will shutdown.");
        System.out.println(" - (maxThreads): Optional  : Max number of Threads used by JCL-Net");
        System.out.println("Example:");
        System.out.println(" java -jar jclServer.jar net=mainnet timeLimit=300 maxThreads=500");
        System.out.println("\n\n");
    }

    private void printStatistics() {
        // We log Statistics:

        log.info("\n--------------------------------------------------------------------------------------------------");
        if (firstTxInstant != null) {
            Duration effectiveTime = Duration.between(firstTxInstant, lastTxInstant);
            log.info("JCL Server :: Statistics > " + Duration.between(firstTxInstant, Instant.now()).toSeconds() + " secs of Test");
            log.info("JCL Server :: Statistics > " + effectiveTime.toSeconds() + " secs processing Txs");
            log.info("JCL Server :: Statistics > performance: " + (numTxs.get() / effectiveTime.toSeconds()) + " txs/sec");
        }
        log.info("JCL Server :: Statistics > " + numINVs.get() + " INVs processed");
        log.info("JCL Server :: Statistics > " + numTxs.get() + " Txs processed");

        log.info("\n--------------------------------------------------------------------------------------------------");
        log.info(" JCL Event Bus Summary: \n" + p2p.getEventBus().getStatus());
    }

    private String formatSize(long numBytes) {
        String result = (sizeTxsLog.get() < 1000)
                ? numBytes + " bytes"
                : (numBytes < 1_000_000)
                ? (numBytes / 1000) + " KB"
                : (numBytes/1_000_000) + " MB";
        return result;
    }

    private void log() {
        // Performance log:
        StringBuffer logLine = new StringBuffer("JCL Server :: Performance : " + numPeersHandshaked + " peers, ");
        if (firstTxInstant != null) {
            int txsPerSec = (int) (((double) numTxsLog.get() / (Duration.between(lastLogInstant, Instant.now()).toMillis())) * 1000);
            logLine.append(numINVs.get() + " INVs, " + numTxs.get() + " Txs received, " + txsPerSec + " txs/sec, " + formatSize(sizeTxsLog.get()) + ", Total: " + formatSize(sizeTxs.get()));
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

        try {
            JCLServerConfig config = JCLServer.getConfigFromArguments(args);
            if (config == null) {
                printHelp();
            } else {
                JCLServer server = new JCLServer(config);
                server.start();
                server.connectToInitialPeers();

                // Shutdown Hook:
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        server.stop();
                    }
                });

                if (config.timeLimit != null) {
                    Thread.sleep(config.timeLimit.toMillis());
                    log.info("JCL Server :: time limit expired. Stopping...");
                    server.stop();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}