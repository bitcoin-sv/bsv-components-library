package com.nchain.jcl.protocol.handlers.message;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.events.*;
import com.nchain.jcl.protocol.events.*;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.protocol.streams.DeserializerStream;
import com.nchain.jcl.protocol.streams.MessageStream;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.streams.StreamDataEvent;
import com.nchain.jcl.tools.streams.StreamErrorEvent;
import com.nchain.jcl.tools.thread.ThreadUtils;
import lombok.Getter;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 15:53
 *
 * Implementation of the MessageHandler.
 * This handler basically makes sure that all the connection to other Peers are registered and wrapped up by
 * a MessageStream (which takes care of the Serializing/Deserializing part), and that the messages received from those
 * peers are publish into the Bus for anybody interested to see.
 */
public class MessageHandlerImpl extends HandlerImpl implements MessageHandler {


    // For logging:
    private LoggerUtil logger;

    // P2P Configuration (used by the MessageStreams) we wrap around each Peer connection
    @Getter private MessageHandlerConfig config;

    // We keep track of all the Connected Peers:
    private Map<PeerAddress, MessagePeerInfo> peersInfo = new ConcurrentHashMap<>();

    // State of this Handler:
    @Getter private MessageHandlerState state = MessageHandlerState.builder().build();

    /** Constructor */
    public MessageHandlerImpl(String id, RuntimeConfig runtimeConfig, MessageHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
    }

    // We register this Handler to LISTEN to these Events:
    private void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(SendMsgRequest.class, e -> onSendMsgReq((SendMsgRequest) e));
        super.eventBus.subscribe(BroadcastMsgRequest.class, e -> onBroadcastReq((BroadcastMsgRequest) e));
        super.eventBus.subscribe(PeerNIOStreamConnectedEvent.class, e -> onPeerStreamConnected((PeerNIOStreamConnectedEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> onPeerDisconnected((PeerDisconnectedEvent) e));
    }

    // Event Handler:
    private void onNetStart(NetStartEvent event) {
        logger.debug("Starting...");
    }
    // Event Handler:
    private void onNetStop(NetStopEvent event) {
        logger.debug("Stop.");
    }
    // Event Handler:
    private void onSendMsgReq(SendMsgRequest request) {
        send(request.getPeerAddress(), request.getBtcMsg());
    }
    // Event Handler:
    private void onBroadcastReq(BroadcastMsgRequest request) {
        broadcast(request.getBtcMsg());
    }
    // Event Handler:
    private void onPeerStreamConnected(PeerNIOStreamConnectedEvent event) {
        PeerAddress peerAddress = event.getStream().getPeerAddress();
        MessageStream msgStream = new MessageStream(ThreadUtils.PEER_STREAM_EXECUTOR, super.runtimeConfig, config.getBasicConfig(), event.getStream());
        msgStream.init();
        // We listen to the Deserializer Events
        msgStream.input().onData(e -> onStreamMsgReceived(peerAddress, e.getData()));
        msgStream.input().onClose( e -> onStreamClosed(peerAddress));
        msgStream.input().onError(e -> onStreamError(peerAddress, e));
        // if a Pre-Serializer has been set, we inject it into this Stream:
        if (config.getPreSerializer() != null)
            ((DeserializerStream) msgStream.input()).setPreSerializer(config.getPreSerializer());

        // We use this Stream to build a MessagePeerInfo and add it to our pool...
        peersInfo.put(event.getStream().getPeerAddress(), new MessagePeerInfo(msgStream));
        // We publish the message to the Bus:
        eventBus.publish(new PeerMsgReadyEvent(msgStream));

        logger.trace(event.getStream().getPeerAddress(), " Peer Stream Connected");
    }
    // Event Handler:
    private void onPeerDisconnected(PeerDisconnectedEvent event) {
        peersInfo.remove(event.getPeerAddress());
    }
    // Event Handler:
    private void onStreamMsgReceived(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
        logger.trace(peerAddress, btcMsg.getHeader().getCommand().toUpperCase() + " Msg received.");
        // We run a basic validation on the message,. If OK we publish the Message on the bus, otherwise we request
        // a Peer disconnection (and a Blacklist??)
        String validationError = findErrorInMsg(btcMsg);
        if (validationError == null) {
            super.eventBus.publish(new MsgReceivedEvent(peerAddress, btcMsg));
            updateState(1, 0);
        } else {
            super.eventBus.publish(new DisconnectPeerRequest(peerAddress, "Wrong Network Id"));
        }
    }
    // Event Handler:
    private void onStreamClosed(PeerAddress peerAddress) {
        peersInfo.remove(peerAddress);
    }

    // Event Handler:
    private void onStreamError(PeerAddress peerAddress, StreamErrorEvent event) {
        // We request a Disconnection from this Peer...
        logger.trace(peerAddress, "Error detected in Stream, requesting disconnection... ");
        super.eventBus.publish(new DisconnectPeerRequest(peerAddress));
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void send(PeerAddress peerAddress, BitcoinMsg<?> btcMessage) {
        if (peersInfo.containsKey(peerAddress)) {
            peersInfo.get(peerAddress).getStream().output().send(new StreamDataEvent<>(btcMessage));
            // We update the state and trigger the event
            updateState(0, 1);
            super.eventBus.publish(new MsgSentEvent(peerAddress, btcMessage));
        } else logger.trace(peerAddress, " Request to Send Msg Discarded (unknown Peer)");

    }

    @Override
    public void broadcast(BitcoinMsg<?> btcMessage) {
        peersInfo.values().forEach(p -> p.getStream().output().send(new StreamDataEvent<>(btcMessage)));
        updateState(0, peersInfo.size());
    }

    // It updates the State of this Handler:
    public synchronized void updateState(long addingMsgsIn, long addingMsgsOut) {
        this.state = this.state.toBuilder()
                .numMsgsIn(state.getNumMsgsIn().add(BigInteger.valueOf(addingMsgsIn)))
                .numMsgsOut(state.getNumMsgsOut().add(BigInteger.valueOf(addingMsgsOut)))
                .build();
    }

    // Very basic Verifications on the Message. If an Error is found, its returned as the result.
    // If the Message is OK, it returns NULL
    private String findErrorInMsg(BitcoinMsg<?> msg) {
        if (msg == null) return "Msg is Empty";
        if (msg.getHeader().getMagic() != config.getBasicConfig().getMagicPackage()) return "Network Id is incorrect";
        return null;
    }

}
