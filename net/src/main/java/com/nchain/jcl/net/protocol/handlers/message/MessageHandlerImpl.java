package com.nchain.jcl.net.protocol.handlers.message;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.*;
import com.nchain.jcl.net.network.streams.StreamDataEvent;
import com.nchain.jcl.net.network.streams.StreamErrorEvent;

import com.nchain.jcl.net.protocol.config.ProtocolVersion;
import com.nchain.jcl.net.protocol.events.control.*;
import com.nchain.jcl.net.protocol.events.control.BroadcastMsgRequest;
import com.nchain.jcl.net.protocol.events.data.MsgReceivedBatchEvent;
import com.nchain.jcl.net.protocol.events.data.MsgReceivedEvent;
import com.nchain.jcl.net.protocol.events.control.SendMsgRequest;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder;
import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.serialization.common.MsgSerializersFactory;
import com.nchain.jcl.net.protocol.streams.MessageStream;
import com.nchain.jcl.net.protocol.streams.deserializer.Deserializer;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStream;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.tools.handlers.HandlerImpl;
import com.nchain.jcl.net.tools.LoggerUtil;
import com.nchain.jcl.tools.thread.ThreadUtils;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
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
public class MessageHandlerImpl extends HandlerImpl<PeerAddress, MessagePeerInfo> implements MessageHandler {

    // For logging:
    private LoggerUtil logger;

    // P2P Configuration (used by the MessageStreams) we wrap around each Peer connection
    private MessageHandlerConfig config;

    // State of this Handler:
    private MessageHandlerState state = MessageHandlerState.builder().build();

    // An instance of a Deserializer. There is ONLY ONE Deserializer for all the Streams in the System.
    private Deserializer deserializer;

    // This executor will take care of the Deserializing of Big Messages, which are the ones big enough so they are
    // managed by "Large" DeSerialisers and ech one runs in a dedicated Thread wo they don't slow down the
    // communication with the rest of the peers:
    private ExecutorService dedicateConnsExecutor;

    // This executor will take care of monitoring th batches of messages that are being stored in the background
    // adn it will push them down he pipeline if the timeout is reached.
    private ExecutorService msgBatchesExecutor;


    // Messages BATCH Configuration:
    // if some Messages Batch config has been specified for some MsgType, we keep track of that Batch status:
    private HashMap<Class, MessageBatchManager> msgsBatchManagers = new HashMap<>();

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
        this.msgBatchesExecutor = ThreadUtils.getSingleThreadExecutorService("JclMessageHandler-Job");
        // The Executor responsible for the deserialization of large messages is a cached one, so Threads are created
        // as we need. For a Stream to be able to use a dedicated Thread, its "realTimeProcessingEnabled" property
        // must be set to TRUE.
        this.dedicateConnsExecutor = ThreadUtils.getCachedThreadExecutorService("JclDeserializer");
        //this.dedicateConnsExecutor = Executors.newCachedThreadPool(ThreadUtils.getThreadFactory("jclDeserializer", Thread.MAX_PRIORITY, true));

        // If some Batch Config has been specified, we instantiate the classes to keep track of their state:
        this.config.getMsgBatchConfigs().entrySet().forEach(entry -> msgsBatchManagers.put(entry.getKey(), new MessageBatchManager(entry.getKey(), entry.getValue())));
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
        logger.trace("Starting...");
        this.msgBatchesExecutor.submit(this::checkPendingBatchesToBroadcast);
    }

    // Event Handler:
    private void onNetStop(NetStopEvent event) {
        this.msgBatchesExecutor.shutdownNow();
        logger.trace("Stop.");
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

        // NOTE: The Executor Service assigned to this Stream used to be the same as the Executor responsible for
        // processing the bytes on each Stream (which was a singleThread).
        // Now we are assigning the same Executor as the EventBus (Which is multi-thread)
        MessageStream msgStream = new MessageStream(super.eventBus.getExecutor(),
                super.runtimeConfig,
                config.getBasicConfig(),
                this.deserializer,
                event.getStream(),
                this.dedicateConnsExecutor,
                this.logger);
        msgStream.init();
        // We listen to the Deserializer Events
        msgStream.input().onData(e -> onStreamMsgReceived(peerAddress, e.getData()));
        msgStream.input().onClose( e -> onStreamClosed(peerAddress));
        msgStream.input().onError(e -> onStreamError(peerAddress, e));
        // if a Pre-Serializer has been set, we inject it into this Stream:
        if (config.getPreSerializer() != null)
            ((DeserializerStream) msgStream.input()).setPreSerializer(config.getPreSerializer());

        // We use this Stream to build a MessagePeerInfo and addBytes it to our pool...
        handlerInfo.put(event.getStream().getPeerAddress(), new MessagePeerInfo(msgStream));
        // We publish the message to the Bus:
        eventBus.publish(new PeerMsgReadyEvent(msgStream));

        logger.trace(event.getStream().getPeerAddress(), "Stream Connected");
    }
    // Event Handler:
    private void onPeerDisconnected(PeerDisconnectedEvent event) {
        handlerInfo.remove(event.getPeerAddress());
    }

    // Event Handler:
    private void onStreamMsgReceived(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
        String msgType = btcMsg.getHeader().getMsgCommand().toUpperCase();
        logger.trace(peerAddress, msgType.toUpperCase() + " Msg received.");

        // We only broadcast the MSg to JCL if it's RIGHT...
        String validationError = findErrorInMsg(btcMsg);
        if (validationError == null) {

            // All incoming Msgs are wrapped up in a MegReceivedEvent:
            MsgReceivedEvent event = EventFactory.buildIncomingEvent(peerAddress, btcMsg);

            // The broadcast method is slightly different if a BATCH is configured for this Message type:
            MessageBatchManager batchManager = this.msgsBatchManagers.get(event.getClass());
            if (batchManager != null) {
                publishBatchMessageToEventBus(batchManager.addEventAndExtractBatch(event));
            } else {
                publishMessageToEventBus(event);
            }

        } else {
            // If the Msg is Incorrect, we disconnect from this Peer
            logger.trace(peerAddress, " ERROR In incoming msg :: " + validationError);
            super.eventBus.publish(new DisconnectPeerRequest(peerAddress, validationError));
        }
    }
    // Event Handler:
    private void onStreamClosed(PeerAddress peerAddress) {
        handlerInfo.remove(peerAddress);
    }

    // Event Handler:
    private void onStreamError(PeerAddress peerAddress, StreamErrorEvent event) {
        // We request a Disconnection from this Peer...
        logger.trace(peerAddress, "Error detected in Stream, requesting disconnection... ");
        super.eventBus.publish(new DisconnectPeerRequest(peerAddress));
    }

    // Event Handler:
    private void onEnablePeerBigMessages(EnablePeerBigMessagesRequest event) {
        MessagePeerInfo messagePeerInfo = this.handlerInfo.get(event.getPeerAddress());
        if (messagePeerInfo != null) {
            ((DeserializerStream) messagePeerInfo.getStream().input()).upgradeBufferSize();
        }
    }

    // Event Handler:
    private void onDisablePeerBigMessages(DisablePeerBigMessagesRequest event) {
        MessagePeerInfo messagePeerInfo = this.handlerInfo.get(event.getPeerAddress());
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
        if (handlerInfo.containsKey(peerAddress)) {
            handlerInfo.get(peerAddress).getStream().output().send(new StreamDataEvent<>(btcMessage));
            logger.trace(peerAddress, btcMessage.getHeader().getMsgCommand().toUpperCase() + " Msg sent.");

            // We propagate this message to the Bus, so other handlers can pick them up if they are subscribed to:
            // NOTE: These Events related to messages sent might not be necessary, and they add some multi-thread
            // pressure, so in the future they might be disabled (for noe we need them for some unit tests):

            Event event = EventFactory.buildOutcomingEvent(peerAddress, btcMessage);
            super.eventBus.publish(event);

            /*
            // we also publish a more "general" event, valid for any outcoming message
            super.eventBus.publish(new MsgSentEvent<>(peerAddress, btcMessage));
            */

            // We update the state:
            updateState(0, 1);
        } else logger.trace(peerAddress, " Request to Send Msg Discarded (unknown Peer)");
    }

    @Override
    public void send(PeerAddress peerAddress, BodyMessage msgBody) {
        if (handlerInfo.containsKey(peerAddress)) {
            BitcoinMsg<?> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), msgBody).build();
            send(peerAddress, btcMsg);
        } else logger.trace(peerAddress, " Request to Send Msg Body Discarded (unknown Peer)");
    }

    @Override
    public void broadcast(BitcoinMsg<?> btcMessage) {
        handlerInfo.values().forEach(p -> p.getStream().output().send(new StreamDataEvent<>(btcMessage)));
        updateState(0, handlerInfo.size());
    }

    @Override
    public void broadcast(BodyMessage msgBody) {
        BitcoinMsg<?> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), msgBody).build();
        handlerInfo.values().forEach(p -> p.getStream().output().send(new StreamDataEvent<>(btcMsg)));
        updateState(0, handlerInfo.size());
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

        // Check the Msg length:
        if (msg.getHeader().getMsgLength() < msg.getBody().getLengthInBytes()) {
            return "Header is undersized";
        }
        if (msg.getHeader().getMsgLength() > msg.getBody().getLengthInBytes()) {
            return "Header is oversized";
        }

        // Checks the checksum:
        if (config.isVerifyChecksum()
                && msg.getHeader().getMsgLength() > 0
                && msg.getHeader().getChecksum() != msg.getBody().getChecksum()) {
            return "Checksum is Wrong";
        }

        // Checks the network specified in magic number:
        if (msg.getHeader().getMagic() != config.getBasicConfig().getMagicPackage()) {
            return "Network Id is incorrect";
        }

        // Checks for 4GB Support:
        if (msg.getLengthInbytes() >= config.getBasicConfig().getThresholdSizeExtMsgs()) {
            if (msg.getHeader().getCommand().equalsIgnoreCase(HeaderMsg.EXT_COMMAND))
                return "Message Larger than 4GB but wrong Command";
            if (this.config.getBasicConfig().getProtocolVersion() < ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                return "Message Larger than 4GB but we are running a Protocol < 70016";
        }
        return null;
    }

    public MessageHandlerConfig getConfig() {
        return this.config;
    }

    public MessageHandlerState getState() {
        return this.state;
    }

    // It publishes the event to the Bus and updares the State
    private void publishMessageToEventBus(MsgReceivedEvent event) {
        super.eventBus.publish(event);                                                              // we publish the specific Event
        super.eventBus.publish(new MsgReceivedEvent(event.getPeerAddress(), event.getBtcMsg()));    // we publish a more generic Event
        updateState(1, 0);                                               // State update
    }

    // It publishes the Batch event to the Bus and updares the State
    private void publishBatchMessageToEventBus(Optional<MsgReceivedBatchEvent> batchEventOpt) {
        batchEventOpt.ifPresent(batchEvent -> {
            super.eventBus.publish(batchEvent);                                     // we publish the specific Event
            updateState(batchEvent.getEvents().size(), 0);            // State update
        });
    }

    private void checkPendingBatchesToBroadcast() {
        final Duration TIMEOUT = Duration.ofMillis(2000);
        try {
            while (true) {
                // we only process those Batches that are clearly inactive:
                msgsBatchManagers.values().stream()
                        .forEach(batch -> {
                            if (Duration.between(batch.getTimestamp(), Instant.now()).compareTo(TIMEOUT) > 0) {
                                publishBatchMessageToEventBus(batch.extractBatchAndReset());
                            }
                });
                Thread.sleep(TIMEOUT.toMillis());
            }
        } catch (InterruptedException ie) {
            logger.error(ie.getMessage(), ie);
            throw new RuntimeException(ie);
        }
    }
}
