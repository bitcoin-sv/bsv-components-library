package com.nchain.jcl.net.protocol.handlers.message;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.*;
import com.nchain.jcl.net.network.streams.StreamDataEvent;
import com.nchain.jcl.net.network.streams.StreamErrorEvent;

import com.nchain.jcl.net.protocol.events.control.*;
import com.nchain.jcl.net.protocol.events.control.BroadcastMsgRequest;
import com.nchain.jcl.net.protocol.events.data.MsgReceivedEvent;
import com.nchain.jcl.net.protocol.events.data.MsgSentEvent;
import com.nchain.jcl.net.protocol.events.control.SendMsgRequest;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.net.protocol.serialization.common.MsgSerializersFactory;
import com.nchain.jcl.net.protocol.streams.MessageStream;
import com.nchain.jcl.net.protocol.streams.deserializer.Deserializer;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStream;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.thread.ThreadUtils;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
    private MessageHandlerConfig config;

    // We keep track of all the Connected Peers:
    private Map<PeerAddress, MessagePeerInfo> peersInfo = new ConcurrentHashMap<>();

    // State of this Handler:
    private MessageHandlerState state = MessageHandlerState.builder().build();

    // An instance of a Deserializer. There is ONLY ONE Deserializer for all the Streams in the System.
    private Deserializer deserializer;

    // A Executor Service to manage dedicated Connections with dedicated Threads:
    private ExecutorService dedicateConnsExecutor;

    /** Constructor */
    public MessageHandlerImpl(String id, RuntimeConfig runtimeConfig, MessageHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
        this.deserializer = Deserializer.getInstance(runtimeConfig, config.getDeserializerConfig());

        // In case the TxRawEnabled is TRUE, we update the MsgSerializersFactory, overriding some serializers
        // with their RAW Versions:
        if (config.isRawTxsEnabled()) {
            MsgSerializersFactory.enableRawSerializers();
        }

        this.dedicateConnsExecutor = ThreadUtils.getFixedThreadExecutorService( "JclDeserializer", config.getMaxNumberDedicatedConnections());
    }

    // We register this Handler to LISTEN to these Events:
    private void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class, e -> onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class, e -> onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(SendMsgRequest.class, e -> onSendMsgReq((SendMsgRequest) e));
        super.eventBus.subscribe(SendMsgBodyRequest.class, e -> onSendMsgBodyReq((SendMsgBodyRequest) e));
        super.eventBus.subscribe(SendMsgListRequest.class, e -> onSendMsgListReq((SendMsgListRequest) e));
        super.eventBus.subscribe(BroadcastMsgRequest.class, e -> onBroadcastReq((BroadcastMsgRequest) e));
        super.eventBus.subscribe(BroadcastMsgBodyRequest.class, e -> onBroadcastReq((BroadcastMsgBodyRequest) e));
        super.eventBus.subscribe(PeerNIOStreamConnectedEvent.class, e -> onPeerStreamConnected((PeerNIOStreamConnectedEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class, e -> onPeerDisconnected((PeerDisconnectedEvent) e));
        super.eventBus.subscribe(EnablePeerBigMessagesRequest.class, e -> onEnablePeerBigMessages((EnablePeerBigMessagesRequest) e));
        super.eventBus.subscribe(DisablePeerBigMessagesRequest.class, e -> onDisablePeerBigMessages((DisablePeerBigMessagesRequest) e));
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
    private void onSendMsgBodyReq(SendMsgBodyRequest request) {
        send(request.getPeerAddress(), request.getMsgBody());
    }

    // Event Handler:
    private void onSendMsgListReq(SendMsgListRequest request) {
        PeerAddress peerAddress = request.getPeerAddress();
        request.getBtcMsgs().forEach(r -> send(peerAddress, r));
    }
    // Event Handler:
    private void onBroadcastReq(BroadcastMsgRequest request) {
        broadcast(request.getBtcMsg());
    }

    // Event Handler:
    private void onBroadcastReq(BroadcastMsgBodyRequest request) {
        broadcast(request.getMsgBody());
    }

    // Event Handler:
    private void onPeerStreamConnected(PeerNIOStreamConnectedEvent event) {
        PeerAddress peerAddress = event.getStream().getPeerAddress();
        MessageStream msgStream = new MessageStream(ThreadUtils.PEER_STREAM_EXECUTOR,
                super.runtimeConfig,
                config.getBasicConfig(),
                this.deserializer,
                event.getStream(),
                this.dedicateConnsExecutor);
        msgStream.init();
        // We listen to the Deserializer Events
        msgStream.input().onData(e -> onStreamMsgReceived(peerAddress, e.getData()));
        msgStream.input().onClose( e -> onStreamClosed(peerAddress));
        msgStream.input().onError(e -> onStreamError(peerAddress, e));
        // if a Pre-Serializer has been set, we inject it into this Stream:
        if (config.getPreSerializer() != null)
            ((DeserializerStream) msgStream.input()).setPreSerializer(config.getPreSerializer());

        // We use this Stream to build a MessagePeerInfo and addBytes it to our pool...
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
        String msgType = btcMsg.getHeader().getCommand().toUpperCase();
        logger.trace(peerAddress, msgType + " Msg received.");
        // We run a basic validation on the message,. If OK we publish the Message on the bus, otherwise we request
        // a Peer disconnection (and a Blacklist??)
        String validationError = findErrorInMsg(btcMsg);
        if (validationError == null) {
            // We propagate this message to the Bus, so other handlers can pick them up if they are subscribed to:
            Event event = EventFactory.buildIncomingEvent(peerAddress, btcMsg);
            super.eventBus.publish(event);

            // We also publish a more "general" msgReceived Event, which covers any incoming message...
            super.eventBus.publish(new MsgReceivedEvent(peerAddress, btcMsg));

            // We update the state:
            updateState(1, 0);
        } else {
            logger.trace(peerAddress, " ERROR In incoming msg :: " + validationError);
            super.eventBus.publish(new DisconnectPeerRequest(peerAddress, validationError));
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

    // Event Handler:
    private void onEnablePeerBigMessages(EnablePeerBigMessagesRequest event) {
        MessagePeerInfo messagePeerInfo = this.peersInfo.get(event.getPeerAddress());
        if (messagePeerInfo != null) {
            ((DeserializerStream) messagePeerInfo.getStream().input()).upgradeBufferSize();
        }
    }

    // Event Handler:
    private void onDisablePeerBigMessages(DisablePeerBigMessagesRequest event) {
        MessagePeerInfo messagePeerInfo = this.peersInfo.get(event.getPeerAddress());
        if (messagePeerInfo != null) {
            ((DeserializerStream) messagePeerInfo.getStream().input()).resetBufferSize();
        }
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void send(PeerAddress peerAddress, BitcoinMsg<?> btcMessage) {
        if (peersInfo.containsKey(peerAddress)) {
            peersInfo.get(peerAddress).getStream().output().send(new StreamDataEvent<>(btcMessage));
            logger.trace(peerAddress.toString() + " :: " + btcMessage.getHeader().getCommand() + " Msg sent.");

            // We propagate this message to the Bus, so other handlers can pick them up if they are subscribed to:
            Event event = EventFactory.buildOutcomingEvent(peerAddress, btcMessage);
            super.eventBus.publish(event);

            // we also publish a more "general" event, valid for any outcoming message
            super.eventBus.publish(new MsgSentEvent<>(peerAddress, btcMessage));

            // We update the state:
            updateState(0, 1);
        } else logger.trace(peerAddress, " Request to Send Msg Discarded (unknown Peer)");
    }

    @Override
    public void send(PeerAddress peerAddress, Message msgBody) {
        if (peersInfo.containsKey(peerAddress)) {
            BitcoinMsg<?> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), msgBody).build();
            send(peerAddress, btcMsg);
        } else logger.trace(peerAddress, " Request to Send Msg Body Discarded (unknown Peer)");
    }

    @Override
    public void broadcast(BitcoinMsg<?> btcMessage) {
        peersInfo.values().forEach(p -> p.getStream().output().send(new StreamDataEvent<>(btcMessage)));
        updateState(0, peersInfo.size());
    }

    @Override
    public void broadcast(Message msgBody) {
        BitcoinMsg<?> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), msgBody).build();
        peersInfo.values().forEach(p -> p.getStream().output().send(new StreamDataEvent<>(btcMsg)));
        updateState(0, peersInfo.size());
    }

    // It updates the State of this Handler:
    public synchronized void updateState(long addingMsgsIn, long addingMsgsOut) {
        this.state = this.state.toBuilder()
                .numMsgsIn(state.getNumMsgsIn().add(BigInteger.valueOf(addingMsgsIn)))
                .numMsgsOut(state.getNumMsgsOut().add(BigInteger.valueOf(addingMsgsOut)))
                .deserializerState(deserializer.getState())
                .build();
    }

    // Very basic Verifications on the Message. If an Error is found, its returned as the result.
    // If the Message is OK, it returns NULL
    private String findErrorInMsg(BitcoinMsg<?> msg) {
        if (msg == null) return "Msg is Empty";
        if (msg.getHeader().getMagic() != config.getBasicConfig().getMagicPackage()) return "Network Id is incorrect";
        return null;
    }

    public MessageHandlerConfig getConfig() {
        return this.config;
    }

    public MessageHandlerState getState() {
        return this.state;
    }


}
