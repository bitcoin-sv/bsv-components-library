package com.nchain.jcl.net.protocol.handlers.pingPong;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.DisconnectPeerRequest;
import com.nchain.jcl.net.network.events.NetStartEvent;
import com.nchain.jcl.net.network.events.NetStopEvent;
import com.nchain.jcl.net.network.events.PeerDisconnectedEvent;
import com.nchain.jcl.net.protocol.events.*;
import com.nchain.jcl.net.protocol.messages.PingMsg;
import com.nchain.jcl.net.protocol.messages.PongMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import com.nchain.jcl.net.tools.NonceUtils;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.thread.ThreadUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

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
public class PingPongHandlerImpl extends HandlerImpl implements PingPongHandler {

    // For logging:
    private LoggerUtil logger;

    // P2P Configuration (used by the MessageStreams) we wrap around each Peer connection
    private PingPongHandlerConfig config;

    // We store info about each Peer:
    private Map<PeerAddress, PingPongPeerInfo> peersInfo = new ConcurrentHashMap<>();

    // State of this Handler
    private PingPongHandlerState state = PingPongHandlerState.builder().build();

    // An Executor, to trigger the "handlePingPongJob" in a different Thread:
    private ExecutorService executor;


    /** Constructor */
    public PingPongHandlerImpl(String id, RuntimeConfig runtimeConfig, PingPongHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.executor = ThreadUtils.getSingleThreadScheduledExecutorService("PingPong-Handler-Job");
    }

    // We register this Handler to LISTEN to these Events:
    private void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> onStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> onStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerHandshakedEvent.class, e -> onPeerHandshaked((PeerHandshakedEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> onPeerDisconnected((PeerDisconnectedEvent) e));
        super.eventBus.subscribe(MsgReceivedEvent.class, e -> onMsgReceived((MsgReceivedEvent) e));
        super.eventBus.subscribe(EnablePingPongRequest.class, e -> onEnablePingPong((EnablePingPongRequest) e));
        super.eventBus.subscribe(DisablePingPongRequest.class, e -> onDisablePingPong((DisablePingPongRequest) e));
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void disablePingPong(PeerAddress peerAddress) {
        PingPongPeerInfo peerInfo = peersInfo.get(peerAddress);
        if (peerInfo != null) peerInfo.disablePingPong();
    }

    @Override
    public void enablePingPong(PeerAddress peerAddress) {
        PingPongPeerInfo peerInfo = peersInfo.get(peerAddress);
        if (peerInfo != null) peerInfo.enablePingPong();
    }

    // Event Handler
    public void onStart(NetStartEvent event) {
        logger.debug(" Starting...");
        this.executor.submit(this::handlePingPongJob);
    }

    // Event Handler
    public void onStop(NetStopEvent event) {
        this.executor.shutdownNow();
        logger.debug("Stop.");
    }

    // Event Handler:
    public void onPeerHandshaked(PeerHandshakedEvent event) {
        peersInfo.put(event.getPeerAddress(), new PingPongPeerInfo(event.getPeerAddress()));
    }

    // Event Handler:
    public void onPeerDisconnected(PeerDisconnectedEvent event) {
        peersInfo.remove(event.getPeerAddress());
    }

    // Event Handler:
    public void onMsgReceived(MsgReceivedEvent event) {
        PingPongPeerInfo peerInfo = peersInfo.get(event.getPeerAddress());
        if (peerInfo != null)  processMsg(event.getBtcMsg(), peerInfo);
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
     * It processes an incoming Msg. It only process PING or PONG Messages, discarding the rest
     */
    private void processMsg(BitcoinMsg<?> btcMsg, PingPongPeerInfo peerInfo) {
        // We update the activity of this Peer:
        peerInfo.updateActivity();

        // We process the Msg:
        if (btcMsg.is(PingMsg.MESSAGE_TYPE)) processPingMsg((BitcoinMsg<PingMsg>) btcMsg, peerInfo);
        else if (btcMsg.is(PongMsg.MESSAGE_TYPE)) processPongMsg((BitcoinMsg<PongMsg>) btcMsg, peerInfo);
    }

    /**
     * Process an incoming PING Message. We just replied sending a PONG
     */
    private void processPingMsg(BitcoinMsg<PingMsg> pingMsg, PingPongPeerInfo peerInfo) {
        logger.trace(peerInfo.getPeerAddress(), "PING received, replying with PONG...");
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
        logger.trace(peerInfo.getPeerAddress(), " Pong Received within time limit.");
        updateState(1);
        peerInfo.reset();
    }

    /**
     * Starts the Ping-Pong connection with this Peer.
     * It sends a Ping to it and updateTimestamp the info we store about this Peer
     */
    private void startPingPong(PingPongPeerInfo peerInfo) {
        logger.trace(peerInfo.getPeerAddress(), "Starting Ping/Pong...");

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
        logger.trace(peerInfo.getPeerAddress(), "Ping/Pong Failed", reason);
        // We request a Disconnection
        super.eventBus.publish(new DisconnectPeerRequest(peerInfo.getPeerAddress()));
        // We remove this Peer
        peersInfo.remove(peerInfo.getPeerAddress());
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
            // First loop level: This job runs forever...
            while (true) {
                // Second loop level: We loop over all our registered peers
                for (PingPongPeerInfo peerInfo : peersInfo.values()) {
                    //logger.trace("Checking pending ping for " + peerInfo.getPeerAddress() + "...");
                    if (!peerInfo.isPingPongDisabled()) {
                        boolean pingSent = peerInfo.getTimePingSent() != null;
                        long now = System.currentTimeMillis();

                        // If we have sent a PING, we check that the time we've been waiting for
                        // the response is still within limits:
                        if (pingSent && (now - peerInfo.getTimePingSent()) > config.getResponseTimeout()) {
                            failPingPon(peerInfo, PingPongFailedEvent.PingPongFailedReason.TIMEOUT);
                            continue;
                        }

                        // If we haven't sent a PING yet, we check if it's time to send it:
                        if (!pingSent && (now - peerInfo.getTimeLastActivity()) > config.getInactivityTimeout()) {
                            this.startPingPong(peerInfo);
                            continue;
                        }
                    } // else logger.trace(peerInfo.getPeerAddress() + ", pingPong disabled.");

                } // for...
                Thread.sleep(1000); // We wait for a while between different executions of this job

            } // While...
        } catch (InterruptedException ie) {
            // In case of an interrupted exception we do nothing, since this will be caused most probably by this
            // handlers stopping (when it stops, it kills all the Threads it launched).
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public PingPongHandlerConfig getConfig() {
        return this.config;
    }

    public PingPongHandlerState getState() {
        return this.state;
    }
}
