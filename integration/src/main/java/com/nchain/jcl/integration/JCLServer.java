package com.nchain.jcl.integration;

import com.nchain.jcl.net.network.config.NetworkConfig;
import com.nchain.jcl.net.network.config.provided.NetworkDefaultConfig;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.config.ProtocolConfig;
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder;
import com.nchain.jcl.net.protocol.events.data.RawTxMsgReceivedEvent;
import com.nchain.jcl.net.protocol.events.data.TxMsgReceivedEvent;
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandler;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandler;
import com.nchain.jcl.net.protocol.wrapper.P2P;
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault;
import com.nchain.jcl.tools.events.EventQueueProcessor;
import com.nchain.jcl.tools.thread.ThreadUtils;
import io.bitcoinj.params.Net;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinj.params.RegTestParams;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 16/07/2021
 */
public class JCLServer {

    // Time when we receive the FIRST and LAST TXs:
    Instant firstTxInstant = null;
    Instant lastTxInstant = null;

    AtomicLong numTxs = new AtomicLong();

    final NetworkParameters NETWORK_PARAMS = new RegTestParams(Net.REGTEST);

    // JCL P2P Service:
    private P2P p2p;

    public JCLServer() {
        // We configure the Runtime configuration:
        RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                .build();

        // We configure the P2P connection:
        ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(NETWORK_PARAMS);

        // We fine-tune the Network Configuration:
        NetworkConfig networkConfig = new NetworkDefaultConfig().toBuilder()
                .maxSocketConnectionsOpeningAtSameTime(100)
                .build();

        // Protocol Basic Configuration: We set up the Range of Peers:
        ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(1))
                .maxPeers(OptionalInt.of(1))
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
                .config(runtimeConfig)
                .config(networkConfig)
                .config(protocolConfig)
                .config(basicConfig)
                .config(messageConfig)
                .config(handshakeConfig)
                .publishStates(Duration.ofSeconds(5))       // we publish all Handler states
                .excludeHandler(PingPongHandler.HANDLER_ID)
                .excludeHandler(BlockDownloaderHandler.HANDLER_ID)
                .build();

        EventQueueProcessor txProcessor = new EventQueueProcessor("tx-Queue", ThreadUtils.EVENT_BUS_EXECUTOR_HIGH_PRIORITY);

        txProcessor.addProcessor(TxMsgReceivedEvent.class, e -> processTX((TxMsgReceivedEvent) e));
        txProcessor.addProcessor(RawTxMsgReceivedEvent.class, e -> processRawTX((RawTxMsgReceivedEvent) e));

        p2p.EVENTS.MSGS.TX.forEach(txProcessor::addEvent);
        p2p.EVENTS.MSGS.TX_RAW.forEach(txProcessor::addEvent);

        p2p.startServer();
    }

    private void processTX(TxMsgReceivedEvent event) {
        if (firstTxInstant == null)
            firstTxInstant = Instant.now();

        lastTxInstant = Instant.now();
        numTxs.incrementAndGet();
    }

    private void processRawTX(RawTxMsgReceivedEvent event) {
        if (firstTxInstant == null)
            firstTxInstant = Instant.now();

        lastTxInstant = Instant.now();
        numTxs.incrementAndGet();
    }


    public static void main(String args[]) {
        new JCLServer();
    }
}