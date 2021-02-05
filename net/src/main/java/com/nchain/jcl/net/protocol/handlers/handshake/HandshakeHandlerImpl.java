package com.nchain.jcl.net.protocol.handlers.handshake;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.*;
import com.nchain.jcl.net.protocol.events.*;
import com.nchain.jcl.net.protocol.config.ProtocolVersion;
import com.nchain.jcl.net.protocol.messages.NetAddressMsg;
import com.nchain.jcl.net.protocol.messages.VarStrMsg;
import com.nchain.jcl.net.protocol.messages.VersionAckMsg;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import com.nchain.jcl.net.tools.NonceUtils;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.tools.log.LoggerUtil;
import lombok.Getter;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class HandshakeHandlerImpl extends HandlerImpl implements HandshakeHandler {

    public static final String HANDLER_ID = "Handshake-Handler";

    // For logging:
    private LoggerUtil logger;

    // P2P configuration:
   @Getter private HandshakeHandlerConfig config;

    // We keep track of all the Peers for which we are performing the handshake:
    private Map<PeerAddress, HandshakePeerInfo> peersInfo = new ConcurrentHashMap<>();

    // Handler State:
    @Getter private HandshakeHandlerState state;

    // Local Address we'll use when building Messages...
    private PeerAddress localAddress;

    // TRUE when the NetStopEvent is detected:
    private boolean isStopping;

    // We keep track of the Events triggered when we reach the minimum set of Handshaked Peers, and when htat number
    // also drops below the minimum...
    boolean minPeersReachedEventSent = false;
    boolean minPeersLostEventSent = false;

    public HandshakeHandlerImpl(String id, RuntimeConfig runtimeConfig, HandshakeHandlerConfig config) {
        super(id, runtimeConfig);
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.config = config;
        // We initialize the State:
        this.state = HandshakeHandlerState.builder().build();
    }

    // We register this Handler to LISTEN to these Events:
    private void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(PeerMsgReadyEvent.class, e -> onPeerMsgReady((PeerMsgReadyEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> onPeerDisconnected((PeerDisconnectedEvent) e));
        super.eventBus.subscribe(MsgReceivedEvent.class, e -> onMsgReceived((MsgReceivedEvent) e));
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public boolean isHandshaked(PeerAddress peerAddress) {
        return (peersInfo.containsKey(peerAddress) && peersInfo.get(peerAddress).isHandshakeAccepted());
    }


    // Event Handler:
    private void onNetStart(NetStartEvent event) {
        logger.debug("Starting...");
        this.localAddress = event.getLocalAddress();
    }

    // Event Handler:
    private void onNetStop(NetStopEvent event) {
        isStopping = true;
        logger.debug("Stop.");
    }

    // Event Handler:
    private void onPeerDisconnected(PeerDisconnectedEvent event) {
        HandshakePeerInfo peerInfo = peersInfo.get(event.getPeerAddress());
        if (peerInfo != null) {

            // If the Peer is currently handshaked, we update the status and trigger an specific event:
            if (peerInfo.isHandshakeAccepted() ) {
                updateStatus(-1, false,false, 0);
                super.eventBus.publish(new PeerHandshakedDisconnectedEvent(peerInfo.getPeerAddress(), peerInfo.getVersionMsgReceived()));
            }

            // We remove if from our Pool:
            this.peersInfo.remove(event.getPeerAddress());

            checkIfTriggerMinPeersEvent(true);
            checkIfWeNeedMoreHandshakes();
        } // if (peerInfo != null)...
    }

    // Event Handler:
    private void onPeerMsgReady(PeerMsgReadyEvent event) {
        PeerAddress peerAddress = event.getStream().getPeerAddress();
        HandshakePeerInfo peerInfo = new HandshakePeerInfo(peerAddress);
        peersInfo.put(peerAddress, peerInfo);

        // If we still need Handshakes, we start the process with this Peer:
        if (!doWeHaveEnoughHandshakes()) startHandshake(peerInfo);

        checkIfWeNeedMoreHandshakes();
    }

    // Event Handler:
    private void onMsgReceived(MsgReceivedEvent event) {
        // If this message is coming from a Peer we don't have anymore, we just discard it
        //System.out.println(">>> - msg received: " + event.getBtcMsg().getBody());
        HandshakePeerInfo peerInfo = peersInfo.get(event.getPeerAddress());
        if (peerInfo == null) {
            logger.trace(event.getPeerAddress(), event.getBtcMsg().getHeader().getCommand().toUpperCase(), " message discarded (Peer already discarded)");
            return;
        }
        BitcoinMsg<?> message = event.getBtcMsg();

        if (message.is(VersionMsg.MESSAGE_TYPE)) processVersionMessage(peerInfo, (BitcoinMsg<VersionMsg>) message);
        else if (message.is(VersionAckMsg.MESSAGE_TYPE)) processAckMessage(peerInfo, (BitcoinMsg<VersionAckMsg>) message);
    }


    /**
     * It processes the VERSION Message received from a Remote Peer. It verifies its content according to our
     * Configuration and updates the Handshake workingState for this Peer accordingly:
     */
    private void processVersionMessage(HandshakePeerInfo peerInfo, BitcoinMsg<VersionMsg> message) {

        VersionMsg versionMsg = message.getBody();

        // We update the Status of this Peer:
        peerInfo.receiveVersionMsg(versionMsg);

        // If The Handshake has been already processed, then this Message is a Duplicate:
        if (peerInfo.isHandshakeAccepted() || peerInfo.isHandshakeRejected()) {
            rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.PROTOCOL_MSG_DUPLICATE, null);
            return;
        }

        // We check the Version number:
        if (message.getBody().getVersion() < ProtocolVersion.ENABLE_VERSION.getBitcoinProtocolVersion()) {
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
                if (message.getBody().getUser_agent().getStr().toUpperCase().indexOf(pattern.toUpperCase()) != -1) {
                    rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_USER_AGENT, message.getBody().getUser_agent().getStr());
                    return;
                }
        }

        if (config.getUserAgentWhitelist() != null) {
            boolean hasOneValidPattern = false;
            for (String pattern : config.getUserAgentWhitelist())
                if (message.getBody().getUser_agent().getStr().toUpperCase().indexOf(pattern.toUpperCase()) != -1) {
                    hasOneValidPattern = true;
                }
            if (!hasOneValidPattern) {
                rejectHandshake(peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_USER_AGENT, message.getBody().getUser_agent().getStr());
                return;
            }
        }

        // If we reach this far, the Version is compatible. We send an ACK Back:
        VersionAckMsg ackMsgBody = VersionAckMsg.builder().build();
        BitcoinMsg<VersionAckMsg> btcAckMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), ackMsgBody).build();
        super.eventBus.publish(new SendMsgRequest(peerInfo.getPeerAddress(), btcAckMsg));

        // And we update the State of this Peer:
        peerInfo.sendACK();

        // After this message is processed, we check the Handshake status:
        if (peerInfo.checkHandshakeOK()) acceptHandshake(peerInfo);
    }

    /**
     * It processes the VERSION_ACK Message received from a Remote Peer. It verifies its content according to our
     * Configuration and updates the Handshake workingState for this Peer accordingly:
     */
    private void processAckMessage(HandshakePeerInfo peerInfo, BitcoinMsg<VersionAckMsg> message) {

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
        logger.trace( peerInfo.getPeerAddress(), " ACK OK according to handshake.");

        // We update the sate of this Peer:
        peerInfo.receiveACK();

        // After this message is processed, we check the Handshake status:
        if (peerInfo.checkHandshakeOK())
            acceptHandshake(peerInfo);
    }


    private synchronized void updateStatus(int addNumberHandhsakes,
                                           boolean requestToResumeConns,
                                           boolean requestToStopConns,
                                           int addNumberHandhsakesFailed) {
        HandshakeHandlerState.HandshakeHandlerStateBuilder stateBuilder = this.state.toBuilder();
        stateBuilder.numCurrentHandshakes(state.getNumCurrentHandshakes() + addNumberHandhsakes);
        stateBuilder.numHandshakesFailed(state.getNumHandshakesFailed().add(BigInteger.valueOf(addNumberHandhsakesFailed)));
        if (requestToResumeConns) {
            stateBuilder.moreConnsRequested(true);
            stateBuilder.stopConnsRequested(false);
        }

        if (requestToStopConns) {
            stateBuilder.stopConnsRequested(true);
            stateBuilder.moreConnsRequested(false);
        }
        this.state = stateBuilder.build();
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

    private void checkIfWeNeedMoreHandshakes() {

        // If the service is stopping, we do nothing...
        if (!isStopping) {



            // If we are still below the minimum range of Handshaked Peers and we had Requested to RESUME the connections, we do it now..
            if (!doWeHaveEnoughHandshakes() && !state.isMoreConnsRequested()) {
                logger.trace("Requesting to Resume Connections...");
                super.eventBus.publish(new ResumeConnectingRequest());
                updateStatus(0, true, false, 0);
            }

            if (doWeHaveEnoughHandshakes() && !state.isStopConnsRequested()) {
                logger.trace("Requesting to Stop Connections...");
                super.eventBus.publish(new StopConnectingRequest());

                // Now, in order to keep the number of connections stable and predictable, we are going to disconnect
                // from those Peers we don't need , since we've already reached the MAX limit:

                logger.trace("Requesting to disconnect any Peers Except the ones already handshaked...");
                List<PeerAddress> peerToKeep = peersInfo.values().stream()
                        .filter(p -> p.isHandshakeAccepted())
                        .map(p -> p.getPeerAddress())
                        .collect(Collectors.toList());
                super.eventBus.publish(DisconnectPeersRequest.builder().peersToKeep(peerToKeep).build());
                updateStatus(0, false, true, 0);
            }
        }
    }

    /**
     * This methods checks the number of current Handshakes, and triggers different events depending on that, ONLY if
     * a MINIMUM Set of Handshaked Peers have been defined in the Configuration
     * - If we reached the
     * @param checkForMinPeersLost
     */
    private synchronized void checkIfTriggerMinPeersEvent(boolean checkForMinPeersLost) {

        if (checkForMinPeersLost && config.getBasicConfig().getMinPeers().isPresent()
                && state.getNumCurrentHandshakes() < config.getBasicConfig().getMinPeers().getAsInt()
                && !minPeersLostEventSent) {
            super.eventBus.publish(new MinHandshakedPeersLostEvent(config.getBasicConfig().getMinPeers().getAsInt()));
            minPeersLostEventSent = true;
            minPeersReachedEventSent = false;
        }

        if (!checkForMinPeersLost && config.getBasicConfig().getMaxPeers().isPresent()
                && state.getNumCurrentHandshakes() >= config.getBasicConfig().getMaxPeers().getAsInt()
                && !minPeersReachedEventSent) {
            super.eventBus.publish(new MinHandshakedPeersReachedEvent( config.getBasicConfig().getMaxPeers().getAsInt()));
            minPeersReachedEventSent = true;
            minPeersLostEventSent = false;
        }
    }

    /**
     * Starts the Handshake with the Remote Peer, sending a VERSION Msg
     */
    private void startHandshake(HandshakePeerInfo peerInfo) {
        logger.trace(peerInfo.getPeerAddress(), "Starting Handshake...");

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
            logger.trace(peerInfo.getPeerAddress(), "Handshake Accepted but not used (already have enough). ");
            super.eventBus.publish(new PeerDisconnectedEvent(peerInfo.getPeerAddress(), PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL));
            return;
        }

        // We update the State:
        updateStatus(1, false, false, 0);

        // If we reach this far, we accept the handshake:

        peerInfo.acceptHandshake();
        logger.trace(peerInfo.getPeerAddress(), "Handshake Accepted (" + state.getNumCurrentHandshakes() + " in total)");

        // We trigger the event:
        super.eventBus.publish(new PeerHandshakedEvent(peerInfo.getPeerAddress(), peerInfo.getVersionMsgReceived()));

        checkIfTriggerMinPeersEvent(false);
        checkIfWeNeedMoreHandshakes();
    }

    /**
     * It rejects the Handshake and disconnects it.
     */
    private void rejectHandshake(HandshakePeerInfo peerInfo, PeerHandshakeRejectedEvent.HandshakedRejectedReason reason, String detail) {

        logger.trace(peerInfo.getPeerAddress(), " Rejecting Handshake", reason, detail);

        // We update the state:
        updateStatus(0,false, false, 1);

        // We notify the event:
        PeerHandshakeRejectedEvent event = PeerHandshakeRejectedEvent.builder()
                .versionMsg(peerInfo.getVersionMsgReceived())
                .peerAddress(peerInfo.getPeerAddress())
                .reason(reason)
                .detail(detail)
                .build();
        super.eventBus.publish(event);

        // We request a disconnection from this Peer:
        DisconnectPeerRequest request = DisconnectPeerRequest.builder()
                .peerAddress(peerInfo.getPeerAddress())
                .reason(PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL)
                .detail(detail)
                .build();
        super.eventBus.publish(request);

        // We remove it from our List of Peers...
        peersInfo.remove(peerInfo.getPeerAddress());
    }
}
