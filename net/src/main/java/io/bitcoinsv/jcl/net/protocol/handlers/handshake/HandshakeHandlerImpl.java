/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.handlers.handshake;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.*;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion;
import io.bitcoinsv.jcl.net.protocol.events.control.*;
import io.bitcoinsv.jcl.net.protocol.events.data.VersionAckMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.events.data.VersionMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.messages.NetAddressMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarStrMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VersionAckMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import io.bitcoinsv.jcl.net.tools.NonceUtils;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.tools.events.EventQueueProcessor;
import io.bitcoinsv.jcl.tools.handlers.HandlerImpl;
import io.bitcoinsv.jcl.tools.log.LoggerUtil;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class HandshakeHandlerImpl extends HandlerImpl<PeerAddress, HandshakePeerInfo> implements HandshakeHandler {

    public static final String HANDLER_ID = "Handshake-Handler";

    // For logging:
    private LoggerUtil logger;

    // P2P configuration:
   private HandshakeHandlerConfig config;


    // Handler State:
    private HandshakeHandlerState state;

    // Local Address we'll use when building Messages...
    private PeerAddress localAddress;

    // TRUE when the NetStopEvent is detected:
    private boolean isStopping;

    // We keep track of the Events triggered when we reach the minimum set of Handshaked Peers, and when htat number
    // also drops below the minimum...
    boolean minPeersReachedEventSent = false;
    boolean minPeersLostEventSent = false;
    boolean maxPeersReachedEventSent = false;

    private Lock lock = new ReentrantLock();

    // The Events captured by this Handler will  e processed in a separate Thread/s, by an EventQueueProcessor, this
    // way we won't slow down the rate at which the eVents are published and processed in the Bus
    private EventQueueProcessor eventQueueProcessor;


    public HandshakeHandlerImpl(String id, RuntimeConfig runtimeConfig, HandshakeHandlerConfig config) {
        super(id, runtimeConfig);
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.config = config;
        // We initialize the State:
        this.state = HandshakeHandlerState.builder().build();

        // We start the EventQueueProcessor. We do not expect many messages (compared to the rest of traffic), so a
        // single Thread will do...
        this.eventQueueProcessor = new EventQueueProcessor("JclHandshakeHandler", ThreadUtils.getSingleThreadScheduledExecutorService("JclHandshakeHandler-EventsConsumers"));
    }

    // We register this Handler to LISTEN to these Events:
    private void registerForEvents() {

        this.eventQueueProcessor.addProcessor(NetStartEvent.class, e -> onNetStart((NetStartEvent) e));
        this.eventQueueProcessor.addProcessor(NetStopEvent.class, e -> onNetStop((NetStopEvent) e));
        this.eventQueueProcessor.addProcessor(PeerMsgReadyEvent.class, e -> onPeerMsgReady((PeerMsgReadyEvent) e));
        this.eventQueueProcessor.addProcessor(PeerDisconnectedEvent.class, e -> onPeerDisconnected((PeerDisconnectedEvent) e));
        this.eventQueueProcessor.addProcessor(VersionMsgReceivedEvent.class, e -> onVersionMessage((VersionMsgReceivedEvent) e));
        this.eventQueueProcessor.addProcessor(VersionAckMsgReceivedEvent.class, e -> onAckMessage((VersionAckMsgReceivedEvent) e));

        super.eventBus.subscribe(NetStartEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(NetStopEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(PeerMsgReadyEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(VersionMsgReceivedEvent.class, e -> this.eventQueueProcessor.addEvent(e));
        super.eventBus.subscribe(VersionAckMsgReceivedEvent.class, e -> this.eventQueueProcessor.addEvent(e));

        this.eventQueueProcessor.start();
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public boolean isHandshaked(PeerAddress peerAddress) {
        return (handlerInfo.containsKey(peerAddress) && handlerInfo.get(peerAddress).isHandshakeAccepted());
    }


    // Event Handler:
    private void onNetStart(NetStartEvent event) {
        logger.debug("Starting...");
        this.localAddress = event.getLocalAddress();

    }

    // Event Handler:
    private void onNetStop(NetStopEvent event) {
        isStopping = true;
        this.eventQueueProcessor.stop();
        logger.debug("Stop.");
    }

    // Event Handler:
    private void onPeerDisconnected(PeerDisconnectedEvent event) {
        try {
            lock.lock();
            HandshakePeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
            if (peerInfo != null) {

                // If the Peer is currently handshaked, we update the status and trigger an specific event:
                if (peerInfo.isHandshakeAccepted()) {
                    logger.debug(peerInfo.getPeerAddress(), " Handshaked Peer disconnected : " + event.getReason().toString());
                    super.eventBus.publish(new PeerHandshakedDisconnectedEvent(peerInfo.getPeerAddress(), peerInfo.getVersionMsgReceived()));
                } else {
                    logger.debug(peerInfo.getPeerAddress(), "Not Handshaked Peer Disconnected : " + event.getReason().toString());
                }

                // We remove if from our Pool:
                this.handlerInfo.remove(event.getPeerAddress());

                // We update the State:
                updateStatus(false,false, peerInfo.getPeerAddress());

                checkIfTriggerPeersEvent(true);
                checkIfWeNeedMoreHandshakes();
            } // if (peerInfo != null)...
        } finally {
            lock.unlock();
        }
    }

    // Event Handler:
    private void onPeerMsgReady(PeerMsgReadyEvent event) {
        try {
            lock.lock();
            PeerAddress peerAddress = event.getStream().getPeerAddress();

            // For some strange reasons, sometimes this event is triggered several times, so in order to
            // prevent from starting the same handshake twice, we check if there is already a handshake in progress
            // with this Peer...
            if (handlerInfo.get(peerAddress) == null) {
                HandshakePeerInfo peerInfo = new HandshakePeerInfo(peerAddress);
                handlerInfo.put(peerAddress, peerInfo);

                // If we still need Handshakes, we start the process with this Peer:
                if (!doWeHaveEnoughHandshakes()) startHandshake(peerInfo);

                checkIfWeNeedMoreHandshakes();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * It processes the VERSION Message received from a Remote Peer. It verifies its content according to our
     * Configuration and updates the Handshake workingState for this Peer accordingly:
     */
    private void onVersionMessage(VersionMsgReceivedEvent event) {

        // If this message is coming from a Peer we don't have anymore, we just discard it
        HandshakePeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
        if (peerInfo == null) {
            logger.debug(event.getPeerAddress(), event.getBtcMsg().getHeader().getCommand().toUpperCase(), " message discarded (Peer already discarded)");
            return;
        }
        logger.debug( peerInfo.getPeerAddress(), " received VersionMsg :: " + event.getBtcMsg().getBody().toString());
        try {
            lock.lock();
            VersionMsg versionMsg = event.getBtcMsg().getBody();

            // We update the Status of this Peer:
            peerInfo.receiveVersionMsg(versionMsg);

            // If The Handshake has been already processed, then this Message is a Duplicate:
            if (peerInfo.isHandshakeAccepted() || peerInfo.isHandshakeRejected()) {
                rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.PROTOCOL_MSG_DUPLICATE, null);
                return;
            }

            // We check the Version number:
            if (versionMsg.getVersion() < ProtocolVersion.ENABLE_VERSION.getBitcoinProtocolVersion()) {
                rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_VERSION, null);
                return;
            }

            // We check the Start Height:
            if (versionMsg.getStart_height() < 0) {
                rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_START_HEIGHT, null);
                return;
            }

            // We check the USER_AGENT:
            if (config.getUserAgentBlacklist() != null) {
                for (String pattern : config.getUserAgentBlacklist())
                    if (versionMsg.getUser_agent().getStr().toUpperCase().indexOf(pattern.toUpperCase()) != -1) {
                        rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_USER_AGENT, versionMsg.getUser_agent().getStr());
                        return;
                    }
            }


            if ((config.getUserAgentWhitelist() != null) && (config.getUserAgentWhitelist().length > 0)) {
                boolean hasOneValidPattern = false;
                for (String pattern : config.getUserAgentWhitelist())
                    if (versionMsg.getUser_agent().getStr().toUpperCase().indexOf(pattern.toUpperCase()) != -1) {
                        hasOneValidPattern = true;
                    }
                if (!hasOneValidPattern) {
                    rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_USER_AGENT, versionMsg.getUser_agent().getStr());
                    return;
                }
            }

            // If we reach this far, the Version is compatible. We send an ACK back
            // NOTE: Before sending the ACK, we make sure the PeerHandshaked event is propagated through JCL
            // by the time the remote Peer receives the message.

            peerInfo.sendACK(); // update this peer state as if the ACK was already sent...

            // We check the Handshake...
            if (peerInfo.checkHandshakeOK()) {
                acceptHandshake(peerInfo);
            }

            // NOW we really send the ACK...
            VersionAckMsg ackMsgBody = VersionAckMsg.builder().build();
            BitcoinMsg<VersionAckMsg> btcAckMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), ackMsgBody).build();
            super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcAckMsg));

        } finally {
            lock.unlock();
        }
    }

    /**
     * It processes the VERSION_ACK Message received from a Remote Peer. It verifies its content according to our
     * Configuration and updates the Handshake workingState for this Peer accordingly:
     */
    private void onAckMessage(VersionAckMsgReceivedEvent event) {

        // If this message is coming from a Peer we don't have anymore, we just discard it
        HandshakePeerInfo peerInfo = handlerInfo.get(event.getPeerAddress());
        if (peerInfo == null) {
            logger.debug(event.getPeerAddress(), event.getBtcMsg().getHeader().getCommand().toUpperCase(), " message discarded (Peer already discarded)");
            return;
        }
        logger.debug( peerInfo.getPeerAddress(), " received VersionACK...");
        try {
            lock.lock();
            // If The Handshake has been already processed, then this Message is a Duplicate:
            if (peerInfo.isHandshakeAccepted() || peerInfo.isHandshakeRejected()) {
                rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.PROTOCOL_MSG_DUPLICATE, null);
                return;
            }

            // We check this Message comes AFTER a VERSION Message sent from us
            if (!peerInfo.isVersionMsgSent()) {
                rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.PROTOCOL_MSG_DUPLICATE, null);
                return;
            }

            // We check that this ACK has not been sent already
            if (peerInfo.isACKReceived()) {
                rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.PROTOCOL_MSG_DUPLICATE, null);
                return;
            }

            // If we reach this far, the ACK is OK:
            // We update the sate of this Peer:
            peerInfo.receiveACK();

            // We check the Handshake...
            if (peerInfo.checkHandshakeOK()) {
                acceptHandshake(peerInfo);
            }

        } finally {
            lock.unlock();
        }
    }


    private synchronized void updateStatus(boolean requestToResumeConns,
                                           boolean requestToStopConns,
                                           PeerAddress anotherPeerHandshakedLost) {
        HandshakeHandlerState.HandshakeHandlerStateBuilder stateBuilder = this.state.toBuilder();
        int numCurrentHandshakes = (int) this.handlerInfo.values().stream()
                .filter(p -> p.isHandshakeAccepted())
                .count();
        int numHandshakesInProgress =  (int) this.handlerInfo.values().stream()
                .filter(p -> ((p.isVersionMsgReceived() || p.isVersionMsgSent()) && !p.isHandshakeAccepted() && !p.isHandshakeRejected()))
                .count();
        BigInteger numHandshakesFailed = BigInteger.valueOf(this.handlerInfo.values().stream()
                .filter(p -> p.isHandshakeRejected())
                .count());
        stateBuilder.numCurrentHandshakes(numCurrentHandshakes);
        stateBuilder.numHandshakesInProgress(numHandshakesInProgress);
        stateBuilder.numHandshakesFailed(numHandshakesFailed);
        if (requestToResumeConns) {
            stateBuilder.moreConnsRequested(true);
            stateBuilder.stopConnsRequested(false);
        }

        if (requestToStopConns) {
            stateBuilder.stopConnsRequested(true);
            stateBuilder.moreConnsRequested(false);
        }

        if (anotherPeerHandshakedLost != null) {
            Set<PeerAddress> peersHandshakedLost = this.state.getPeersHandshakedLost();
            peersHandshakedLost.add(anotherPeerHandshakedLost);
            stateBuilder.peersHandshakedLost(peersHandshakedLost);
        }

        this.state = stateBuilder.build();
    }

    private synchronized void updateStatus(boolean requestToResumeConns, boolean requestToStopConns) {
        updateStatus(requestToResumeConns, requestToStopConns, null);
    }

    /**
     * Indicates whether we already have enough Handshakes. This is only TRUE when we've already reached the MAX defined
     * in the Configuration. until then, we'll always be looking for new Peers to Connect to.
     */
    private boolean doWeHaveEnoughHandshakes() {
        int numHandshakes = state.getNumCurrentHandshakes();
        OptionalInt max = config.getBasicConfig().getMaxPeers();
        boolean result = max.isPresent()
                ? numHandshakes >= max.getAsInt()
                : false;
        return result;
    }

    private synchronized void checkIfWeNeedMoreHandshakes() {

        try {
            lock.lock();
            // If the service is stopping, we do nothing...
            if (!isStopping) {

                // If we are still below the minimum range of Handshaked Peers and we had Requested to RESUME the connections, we do it now..
                if (!doWeHaveEnoughHandshakes() && !state.isMoreConnsRequested()) {
                    logger.debug("Requesting to Resume Connections...");
                    super.eventBus.publish(new ResumeConnectingRequest());
                    updateStatus( true, false);
                }

                if (doWeHaveEnoughHandshakes() && !state.isStopConnsRequested()) {
                    logger.debug("Requesting to Stop Connections...");
                    super.eventBus.publish(new StopConnectingRequest());

                    // Now, in order to keep the number of connections stable and predictable, we are going to disconnect
                    // from those Peers we don't need , since we've already reached the MAX limit:

                    logger.debug("Requesting to disconnect any Peers Except the ones already handshaked...");
                    List<PeerAddress> peerToKeep = handlerInfo.values().stream()
                            .filter(p -> p.isHandshakeAccepted())
                            .map(p -> p.getPeerAddress())
                            .collect(Collectors.toList());
                    super.eventBus.publish(DisconnectPeersRequest.builder().peersToKeep(peerToKeep).build());
                    updateStatus( false, true);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * This methods checks the number of current Handshakes, and triggers different events depending on that:
     */
    private synchronized void checkIfTriggerPeersEvent(boolean connectionsDecreasing) {

        // We check if we need to trigger an event about the minPeers being Reached or Lost:

        if (config.getBasicConfig().getMinPeers().isPresent()) {
            // minHandshakedPeersLost
            if (state.getNumCurrentHandshakes() < config.getBasicConfig().getMinPeers().getAsInt()
                    && !minPeersLostEventSent
                    && connectionsDecreasing) {
                super.eventBus.publish(new MinHandshakedPeersLostEvent(state.getNumCurrentHandshakes()));
                minPeersLostEventSent = true;
                minPeersReachedEventSent = false;
                maxPeersReachedEventSent = false;
            }
            // minHandshakedPeersReached
            if (state.getNumCurrentHandshakes() >=config.getBasicConfig().getMinPeers().getAsInt()
                    && !minPeersReachedEventSent
                    && !connectionsDecreasing) {
                super.eventBus.publish(new MinHandshakedPeersReachedEvent(state.getNumCurrentHandshakes()));
                minPeersReachedEventSent = true;
                minPeersLostEventSent = false;
            }
        }

        // We check if we need to trigger an event about the maxPeers being Reached or Lost:

        if (config.getBasicConfig().getMaxPeers().isPresent()) {

            // maxHandshakedPeersReached
            if (state.getNumCurrentHandshakes() >=config.getBasicConfig().getMaxPeers().getAsInt()
                    && !maxPeersReachedEventSent
                    && !connectionsDecreasing) {
                super.eventBus.publish(new MaxHandshakedPeersReachedEvent(state.getNumCurrentHandshakes()));
                maxPeersReachedEventSent = true;
            }
        }
    }

    /**
     * Starts the Handshake with the Remote Peer, sending a VERSION Msg
     */
    private void startHandshake(HandshakePeerInfo peerInfo) {
        logger.debug(peerInfo.getPeerAddress(), "Starting Handshake...");

        VarStrMsg userAgentMsg = VarStrMsg.builder().str( config.getUserAgent()).build();
        NetAddressMsg addr_from = NetAddressMsg.builder()
                .address(localAddress)
                .timestamp(System.currentTimeMillis())
                .build();
        NetAddressMsg addr_recv = NetAddressMsg.builder()
                .address(peerInfo.getPeerAddress())
                .timestamp(System.currentTimeMillis())
                .build();
        VersionMsg versionMsg = VersionMsg.builder()
                .user_agent(userAgentMsg)
                .version(config.getBasicConfig().getProtocolVersion())
                .relay(config.isRelayTxs())
                .services(config.getServicesSupported())
                .addr_from(addr_from)
                .addr_recv(addr_recv)
                .start_height(config.getBlock_height())
                .nonce(NonceUtils.newOnce())
                .timestamp(System.currentTimeMillis())
                .build();
        BitcoinMsg<VersionMsg> btcVersionMsg = new BitcoinMsgBuilder<VersionMsg>(config.getBasicConfig(), versionMsg).build();
        super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcVersionMsg));
        peerInfo.sendVersionMsg(versionMsg);
    }

    /**
     * It registers the new Handshake, and triggers the callbacks to inform about a new successfull handshake performed.
     */
    private void acceptHandshake(HandshakePeerInfo peerInfo) {


        // Check that we have not broken the MAX limit. If we do, we request a disconnection from this Peer
        if (config.getBasicConfig().getMaxPeers().isPresent() && state.getNumCurrentHandshakes() >= config.getBasicConfig().getMaxPeers().getAsInt()) {
            logger.debug(peerInfo.getPeerAddress(), " Handshake Accepted but not used (already have enough). ");
            super.eventBus.publish(new DisconnectPeerRequest(peerInfo.getPeerAddress(), "too many handshakes"));
            return;
        }

        // If we reach this far, we accept the handshake:
        peerInfo.acceptHandshake();

        // We update the State:
        updateStatus(false, false);
        logger.debug(peerInfo.getPeerAddress(), "Handshake Accepted (" + state.getNumCurrentHandshakes() + " in total)");

        // We trigger the event:
        super.eventBus.publish(new PeerHandshakedEvent(peerInfo.getPeerAddress(), peerInfo.getVersionMsgReceived()));

        checkIfTriggerPeersEvent(false);
        checkIfWeNeedMoreHandshakes();
    }

    /**
     * It rejects the Handshake and disconnects it.
     */
    private void rejectHandshake(HandshakePeerInfo peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason reason, String detail) {

        peerInfo.rejectHandshake();
        logger.debug(peerInfo.getPeerAddress(), " Rejecting Handshake", reason, detail);

        // We update the state:
        updateStatus(false, false);

        // We notify the event:
        super.eventBus.publish(
                new PeerHandshakeRejectedEvent(
                        peerInfo.getPeerAddress(),
                        peerInfo.getVersionMsgReceived(),
                        reason,
                        detail
                )
        );

        // We request a disconnection from this Peer:
        DisconnectPeerRequest request = new DisconnectPeerRequest(
                peerInfo.getPeerAddress(),
                PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL,
                detail);
        super.eventBus.publish(request);

        // We remove it from our List of Peers...
        handlerInfo.remove(peerInfo.getPeerAddress());
    }

    public HandshakeHandlerConfig getConfig() {
        return this.config;
    }

    public HandshakeHandlerState getState() {
        return this.state;
    }
}
