package io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong;


import io.bitcoinsv.bsvcl.net.protocol.messages.PingMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.events.DisconnectPeerRequest;
import io.bitcoinsv.bsvcl.net.network.events.NetStartEvent;
import io.bitcoinsv.bsvcl.net.network.events.NetStopEvent;
import io.bitcoinsv.bsvcl.net.network.events.PeerDisconnectedEvent;
import io.bitcoinsv.bsvcl.net.protocol.events.control.DisablePingPongRequest;
import io.bitcoinsv.bsvcl.net.protocol.events.control.EnablePingPongRequest;
import io.bitcoinsv.bsvcl.net.protocol.events.control.PeerHandshakedEvent;
import io.bitcoinsv.bsvcl.net.protocol.events.control.PingPongFailedEvent;
import io.bitcoinsv.bsvcl.net.protocol.events.control.SendMsgRequest;
import io.bitcoinsv.bsvcl.net.protocol.events.data.MsgReceivedEvent;
import io.bitcoinsv.bsvcl.net.tools.NonceUtils;
import io.bitcoinsv.bsvcl.tools.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.tools.events.EventQueueProcessor;
import io.bitcoinsv.bsvcl.tools.handlers.HandlerConfig;
import io.bitcoinsv.bsvcl.tools.handlers.HandlerImpl;
import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;
import io.bitcoinsv.bsvcl.tools.thread.ThreadUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of the Ping/Pong P2P Handler.
 * This handler keeps track of tal the Handshaked Peers, and starts the Handshake for them if they've been inactive
 * for a period of time longer than the threshold secifie din the P2P Configuration. The Ping/Pong protocol can
 * also be disabled, in case we assume that the remote Peer is gonna be very busy in the future (for example, if we
 * are downloading a block from it), and we dont want to overload it with replying to our Ping messages.
 */
public class PingPongHandlerImpl extends HandlerImpl<PeerAddress, PingPongPeerInfo> implements PingPongHandler {

    // For logging:
    private LoggerUtil logger;

    // P2P Configuration (used by the MessageStreams) we wrap around each Peer connection
    private PingPongHandlerConfig config;

    // State of this Handler
    private PingPongHandlerState state = PingPongHandlerState.builder().build();

    // An Executor, to trigger the "handlePingPongJob" in a different Thread:
    private ScheduledExecutorService executor;

    // The Events captured by this Handler will  e processed in a separate Thread/s, by an EventQueueProcessor, this
    // way we won't slow down the rate at which the eVents are published and processed in the Bus
    private EventQueueProcessor eventQueueProcessor;

    /** Constructor */
    public PingPongHandlerImpl(String id, RuntimeConfig runtimeConfig, PingPongHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getSingleThreadScheduledExecutorService("JclPingPongHandler");

        // TODO: if required make capacity configurable
        // We start the EventQueueProcessor. We do not expect many messages (compared to the rest of traffic), so a
        // single Thread will do...
        this.eventQueueProcessor = new EventQueueProcessor("JclPingPongHandler", ThreadUtils.getBlockingThreadExecutorService("JclPingPongHandler-EventsConsumers", 3, 6));
    }

    // We register this Handler to LISTEN to these Events:
    private void registerForEvents() {

        this.eventQueueProcessor.addProcessor(NetStartEvent.class, this::onStart);
        this.eventQueueProcessor.addProcessor(NetStopEvent.class, this::onStop);
        this.eventQueueProcessor.addProcessor(PeerHandshakedEvent.class, this::onPeerHandshaked);
        this.eventQueueProcessor.addProcessor(PeerDisconnectedEvent.class, this::onPeerDisconnected);
        this.eventQueueProcessor.addProcessor(MsgReceivedEvent.class, this::onMsgReceived);
        this.eventQueueProcessor.addProcessor(EnablePingPongRequest.class, this::onEnablePingPong);
        this.eventQueueProcessor.addProcessor(DisablePingPongRequest.class, this::onDisablePingPong);

        subscribe(NetStartEvent.class, eventQueueProcessor::addEvent);
        subscribe(NetStopEvent.class, eventQueueProcessor::addEvent);
        subscribe(PeerHandshakedEvent.class, eventQueueProcessor::addEvent);
        subscribe(PeerDisconnectedEvent.class, eventQueueProcessor::addEvent);
        subscribe(MsgReceivedEvent.class, eventQueueProcessor::addEvent);
        subscribe(EnablePingPongRequest.class, eventQueueProcessor::addEvent);
        subscribe(DisablePingPongRequest.class, eventQueueProcessor::addEvent);

        this.eventQueueProcessor.start();
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void disablePingPong(PeerAddress peerAddress) {
        PingPongPeerInfo peerInfo = getOrWaitForHandlerInfo(peerAddress);
        if(peerInfo != null) {
            peerInfo.disablePingPong();
        }
    }

    @Override
    public void enablePingPong(PeerAddress peerAddress) {
        PingPongPeerInfo peerInfo = getOrWaitForHandlerInfo(peerAddress);
        if(peerInfo != null) {
            peerInfo.enablePingPong();
        }
    }

    // Event Handler
    public void onStart(NetStartEvent event) {
        logger.trace("Starting...");
        this.executor.scheduleAtFixedRate(this::handlePingPongJob, 0L, 1L, TimeUnit.SECONDS);
    }

    // Event Handler
    public void onStop(NetStopEvent event) {
        this.executor.shutdownNow();
        this.eventQueueProcessor.stop();
        logger.trace("Stop.");
    }

    // Event Handler:
    public void onPeerHandshaked(PeerHandshakedEvent event) {
        handlerInfo.put(event.getPeerAddress(), new PingPongPeerInfo(event.getPeerAddress()));
    }

    // Event Handler:
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        handlerInfo.remove(event.getPeerAddress());
    }


    // Event Handler
    public void onMsgReceived(MsgReceivedEvent event) {
        PingPongPeerInfo peerInfo = getOrWaitForHandlerInfo(event.getPeerAddress());

        if (peerInfo != null) {
            // We update the activity of this Peer and process it:
            peerInfo.updateActivity();
            if (event.getBtcMsg().is(PingMsg.MESSAGE_TYPE)) {
                processPingMsg(event.getBtcMsg(), peerInfo);
            } else if (event.getBtcMsg().is(PongMsg.MESSAGE_TYPE)){
                processPongMsg(event.getBtcMsg(), peerInfo);
            }
        } else if (event.getBtcMsg().is(PingMsg.MESSAGE_TYPE)) {
            BitcoinMsg<PingMsg> pingMsg = (BitcoinMsg<PingMsg>) event.getBtcMsg();
            logger.debug(peerInfo.getPeerAddress(), "PING received from UNKNON PEER, replying with PONG...");
            PongMsg pongMSg = PongMsg.builder().nonce(pingMsg.getBody().getNonce()).build();
            BitcoinMsg<PongMsg> btcPongMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), pongMSg).build();
            super.eventBus.publish(new SendMsgRequest(event.getPeerAddress(), btcPongMsg));
        }
    }

    // Event Handler:
    public void onEnablePingPong(EnablePingPongRequest event) {
        enablePingPong(event.getPeerAddress());
    }

    // Event Handler:
    public void onDisablePingPong(DisablePingPongRequest event) {
        disablePingPong(event.getPeerAddress());
    }

    /**
     * Process an incoming PING Message. We just replied sending a PONG
     */
    private void processPingMsg(BitcoinMsg<PingMsg> pingMsg, PingPongPeerInfo peerInfo) {
        logger.debug(peerInfo.getPeerAddress(), "PING received, replying with PONG...");
        PongMsg pongMSg = PongMsg.builder().nonce(pingMsg.getBody().getNonce()).build();
        BitcoinMsg<PongMsg> btcPongMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), pongMSg).build();
        super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcPongMsg));
    }

    /**
     * Process an incoming PONG Message. We check that the message is consistent with the Ping/Pong P2P
     */
    private void processPongMsg(BitcoinMsg<PongMsg> pongMsg, PingPongPeerInfo peerInfo) {
        // We check that we have sent a PING message before this Pong:
        if (peerInfo.getTimePingSent() == null) {
            failPingPon(peerInfo, PingPongFailedEvent.PingPongFailedReason.MISSING_PING);
            return;
        }

        // We check that the NONCE in this PONG matches the one in the previous PING
        if (peerInfo.getTimePingSent() != null && !peerInfo.getNoncePingSent().equals(pongMsg.getBody().getNonce())) {
            failPingPon(peerInfo, PingPongFailedEvent.PingPongFailedReason.WRONG_NONCE);
            return;
        }

        // If we reach this far, the Ping-Pong is CORRECT. We update the state and reset the Peer:
        logger.debug(peerInfo.getPeerAddress(), "PONG Received within time limit.");
        updateState(1);
        peerInfo.reset();
    }

    /**
     * Starts the Ping-Pong connection with this Peer.
     * It sends a Ping to it and updateTimestamp the info we store about this Peer
     */
    private void startPingPong(PingPongPeerInfo peerInfo) {
        logger.debug(peerInfo.getPeerAddress(), "Starting Ping/Pong...");

        // We send a PING Message to this Peer:
        PingMsg pingMsg = PingMsg.builder().nonce(NonceUtils.newOnce()).build();
        BitcoinMsg<PingMsg> btcPingMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), pingMsg).build();
        super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcPingMsg));

        // We update this Peer Info:
        peerInfo.updatePingStarted(pingMsg);
    }

    /**
     * It fails the PingPong: Disconnects the Peer and forEach the callbacks to notify about this event
     */
    private void failPingPon(PingPongPeerInfo peerInfo, PingPongFailedEvent.PingPongFailedReason reason) {
        logger.debug(peerInfo.getPeerAddress(), "Ping/Pong Failed", reason);
        // We request a Disconnection
        super.eventBus.publish(new DisconnectPeerRequest(peerInfo.getPeerAddress()));
        // We remove this Peer
        handlerInfo.remove(peerInfo.getPeerAddress());
        // We propagate the event
        super.eventBus.publish(new PingPongFailedEvent(peerInfo.getPeerAddress(), reason));
    }

    // It updates the Handler State:
    private synchronized void updateState(int numPingsInProgressToAdd) {
        this.state = this.state.toBuilder()
                .numPingInProcess(state.getNumPingInProcess() + numPingsInProgressToAdd)
                .build();
    }

    /**
     * This metod implements the Job that is running in a separate Thread and takes care of looking into
     * the Peers we have registered, to check if we should startTime the Ping-Pong connection on them, or just
     * discard them if the connection has failed (due to a timeout, etc)
     */
    private void handlePingPongJob() {
        try {
            // First loop level: We loop over all our registered peers
            for (PingPongPeerInfo peerInfo : handlerInfo.values()) {
                //logger.trace("Checking pending ping for " + peerInfo.getPeerAddress() + "...");
                if (!peerInfo.isPingPongDisabled()) {
                    boolean pingSent = peerInfo.getTimePingSent() != null;
                    Instant now = Instant.now();
                    // If we have sent a PING, we check that the time we've been waiting for
                    // the response is still within limits:
                    if (pingSent && Duration.between(peerInfo.getTimePingSent(), now).compareTo(config.getResponseTimeout()) > 0) {
                        failPingPon(peerInfo, PingPongFailedEvent.PingPongFailedReason.TIMEOUT);
                        continue;
                    }

                    // If we haven't sent a PING yet, we check if it's time to send it:
                    if (!pingSent && (Duration.between(peerInfo.getTimeLastActivity(), now).compareTo(config.getInactivityTimeout()) > 0)) {
                        this.startPingPong(peerInfo);
                        continue;
                    }
                } // else logger.trace(peerInfo.getPeerAddress() + ", pingPong disabled.");

            } // for...
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public PingPongHandlerConfig getConfig() {
        return this.config;
    }

    @Override
    public synchronized void updateConfig(HandlerConfig config) {
        if (!(config instanceof PingPongHandlerConfig)) {
            throw new RuntimeException("config class is NOT correct for this Handler");
        }
        this.config = (PingPongHandlerConfig) config;
    }

    public PingPongHandlerState getState() {
        return this.state;
    }
}