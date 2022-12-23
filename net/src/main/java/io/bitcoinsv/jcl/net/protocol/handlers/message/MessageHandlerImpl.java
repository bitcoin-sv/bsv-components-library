package io.bitcoinsv.jcl.net.protocol.handlers.message;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.*;
import io.bitcoinsv.jcl.net.network.streams.StreamDataEvent;
import io.bitcoinsv.jcl.net.network.streams.StreamErrorEvent;

import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion;
import io.bitcoinsv.jcl.net.protocol.events.control.*;
import io.bitcoinsv.jcl.net.protocol.events.data.MsgReceivedBatchEvent;
import io.bitcoinsv.jcl.net.protocol.events.data.MsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.messages.ByteStreamMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.*;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MsgSerializersFactory;
import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.MessageStream;
import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer.Deserializer;
import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerStream;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayBuffer;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.tools.events.Event;
import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;
import io.bitcoinsv.jcl.tools.handlers.HandlerImpl;
import io.bitcoinsv.jcl.net.tools.LoggerUtil;
import io.bitcoinsv.jcl.tools.thread.ThreadUtils;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
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

    //This executor will take care of the broadcasting of messages. If we're streaming a large block to a peer, then we don't
    //want to block other messages from being sent while we wait for the large block to be sent
    private ExecutorService broadcastExecutor;

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

        broadcastExecutor = ThreadUtils.getCachedThreadExecutorService("jclBroadcaster", 4);

        // If some Batch Config has been specified, we instantiate the classes to keep track of their state:
        this.config.getMsgBatchConfigs().entrySet().forEach(entry -> msgsBatchManagers.put(entry.getKey(), new MessageBatchManager(entry.getKey(), entry.getValue())));

    }

    // We register this Handler to LISTEN to these Events:
    private void registerForEvents() {
        super.eventBus.subscribe(NetStartEvent.class,                   e -> onNetStart((NetStartEvent) e));
        super.eventBus.subscribe(NetStopEvent.class,                    e -> onNetStop((NetStopEvent) e));
        super.eventBus.subscribe(SendMsgRequest.class,                  e -> onSendMsgReq((SendMsgRequest) e));
        super.eventBus.subscribe(SendMsgBodyRequest.class,              e -> onSendMsgBodyReq((SendMsgBodyRequest) e));
        super.eventBus.subscribe(SendMsgListRequest.class,              e -> onSendMsgListReq((SendMsgListRequest) e));
        super.eventBus.subscribe(BroadcastMsgRequest.class,             e -> onBroadcastReq((BroadcastMsgRequest) e));
        super.eventBus.subscribe(BroadcastMsgBodyRequest.class,         e -> onBroadcastReq((BroadcastMsgBodyRequest) e));
        super.eventBus.subscribe(PeerNIOStreamConnectedEvent.class,     e -> onPeerStreamConnected((PeerNIOStreamConnectedEvent) e));
        super.eventBus.subscribe(PeerDisconnectedEvent.class,           e -> onPeerDisconnected((PeerDisconnectedEvent) e));
        super.eventBus.subscribe(EnablePeerBigMessagesRequest.class,    e -> onEnablePeerBigMessages((EnablePeerBigMessagesRequest) e));
        super.eventBus.subscribe(DisablePeerBigMessagesRequest.class,   e -> onDisablePeerBigMessages((DisablePeerBigMessagesRequest) e));
        super.eventBus.subscribe(PeerHandshakedEvent.class,             e -> onPeerHandshaked((PeerHandshakedEvent) e));

        super.eventBus.subscribe(SendMsgHandshakedRequest.class,        e -> onSendMsgHandshaked((SendMsgHandshakedRequest) e));
        super.eventBus.subscribe(SendMsgBodyHandshakedRequest.class,    e -> onSendMsgBodyHandshaked((SendMsgBodyHandshakedRequest) e));
        super.eventBus.subscribe(BroadcastMsgHandshakedRequest.class,   e -> onBroadcastMsgHandshaked((BroadcastMsgHandshakedRequest) e));
        super.eventBus.subscribe(BroadcastMsgBodyHandshakedRequest.class, e -> onBroadcastMsgBodyHandshaked((BroadcastMsgBodyHandshakedRequest) e));
        super.eventBus.subscribe(SendMsgListHandshakeRequest.class,     e -> onSendMsgListHandshakeReq((SendMsgListHandshakeRequest) e));
        this.eventBus.subscribe(SendMsgStreamHandshakeRequest.class,    e -> onSendMsgStreamHandshakeRequest((SendMsgStreamHandshakeRequest) e));
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

        // NOTE: We can process incoming messages in any order, as larger messages are handled by the serializers. For outgoing streams, they need to be processed in the order
        // they're submitted since larger messages are split into chunks
        MessageStream msgStream = new MessageStream(
                super.eventBus.getExecutor(),
                super.runtimeConfig,
                config,
                this.deserializer,
                event.getStream(),
                this.dedicateConnsExecutor,
                this.logger);
        msgStream.init();
        // We listen to the Deserializer Events
        msgStream.input().onData(e -> {

            switch (e.getData().getMessageType()) {
                case BitcoinMsg.MESSAGE_TYPE:
                    onStreamMsgReceived(peerAddress, (BitcoinMsg<?>) e.getData());
                break;

                default:
                    logger.warm("Unhandled Message Type: " + e.getData().getMessageType().toUpperCase());
            }
        });

        msgStream.input().onClose( e -> onStreamClosed(peerAddress));
        msgStream.input().onError(e -> onStreamError(peerAddress, e));
        // if a Pre-Serializer has been set, we inject it into this Stream:
        if (config.getPreSerializer() != null)
            ((DeserializerStream) msgStream.input()).setPreSerializer(config.getPreSerializer());

        // We use this Stream to build a MessagePeerInfo and add it to our pool...
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
    private void onStreamMsgReceived(PeerAddress peerAddress, BitcoinMsg<?> bitcoinMsg) {
        String msgType = bitcoinMsg.getBody().getMessageType().toUpperCase();
        logger.trace(peerAddress, msgType.toUpperCase() + " Msg received.");


        // We only broadcast the MSg to JCL if it's RIGHT...
        String validationError = findErrorInMsg(bitcoinMsg);
        if (validationError == null) {

            // All incoming Msgs are wrapped up in a MsgReceivedEvent:
            MsgReceivedEvent event = EventFactory.buildIncomingEvent(peerAddress, bitcoinMsg);

            // The broadcast method is slightly different if a BATCH is configured for this Message type:
            MessageBatchManager batchManager = this.msgsBatchManagers.get(event.getClass());
            if (batchManager != null) {
                publishBatchMessageToEventBus(batchManager.addEventAndExtractBatch(event));
            } else {
                publishMessageToEventBus(event);
            }

        } else {
            // If the Msg is Incorrect, we disconnect from this Peer
            logger.error(peerAddress, "ERROR In incoming " + msgType + " msg :: " + validationError);
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

    // Event Handler:
    private void onPeerHandshaked(PeerHandshakedEvent event) {
        MessagePeerInfo messagePeerInfo = this.handlerInfo.get(event.getPeerAddress());
        if (messagePeerInfo != null) {
            messagePeerInfo.handshake();
        }
    }

    // Event Handler
    public void onSendMsgHandshaked(SendMsgHandshakedRequest event) {
        MessagePeerInfo messagePeerInfo = this.handlerInfo.get(event.getPeerAddress());
        if ((messagePeerInfo != null) && messagePeerInfo.isHandshaked()) {
            send(event.getPeerAddress(), event.getBtcMsg());
        }
    }

    // Event Handler
    public void onSendMsgBodyHandshaked(SendMsgBodyHandshakedRequest event) {
        MessagePeerInfo messagePeerInfo = this.handlerInfo.get(event.getPeerAddress());
        if ((messagePeerInfo != null) && messagePeerInfo.isHandshaked()) {
            send(event.getPeerAddress(), event.getMsgBody());
        }
    }

    // Event Handler
    public void onBroadcastMsgHandshaked(BroadcastMsgHandshakedRequest event) {
        handlerInfo.values().stream()
                .filter(MessagePeerInfo::isHandshaked)
                .forEach(p -> send(p.getStream().getPeerAddress(), event.getBtcMsg()));
    }

    // Event Handler
    public void onBroadcastMsgBodyHandshaked(BroadcastMsgBodyHandshakedRequest event) {
        handlerInfo.values().stream()
                .filter(MessagePeerInfo::isHandshaked)
                .forEach(p -> send(p.getStream().getPeerAddress(), event.getMsgBody()));
    }

    // Event Handler:
    private void onSendMsgListHandshakeReq(SendMsgListRequest request) {
        MessagePeerInfo messagePeerInfo = this.handlerInfo.get(request.getPeerAddress());
        if ((messagePeerInfo != null) && messagePeerInfo.isHandshaked()) {
            request.getBtcMsgs().forEach(r -> send(request.getPeerAddress(), r));
        }
    }

    private void onSendMsgStreamHandshakeRequest(SendMsgStreamHandshakeRequest request) {
        MessagePeerInfo messagePeerInfo = this.handlerInfo.get(request.getPeerAddress());
        if ((messagePeerInfo != null) && messagePeerInfo.isHandshaked()) {
            stream(request.getPeerAddress(), request.getStreamRequest());
        }
    }

    @Override
    public void init() {
        registerForEvents();
    }

    @Override
    public void send(PeerAddress peerAddress, BitcoinMsg<?> btcMessage) {
        _send(peerAddress, btcMessage);
    }

    @Override
    public void send(PeerAddress peerAddress, BodyMessage msgBody) {
        BitcoinMsg<?> btcMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), msgBody).build();
        send(peerAddress, btcMsg);
    }

    private void _send(PeerAddress peerAddress, Message message){
        synchronized (peerAddress.toString().intern()) {
            if (handlerInfo.containsKey(peerAddress)) {
                //send the message
                handlerInfo.get(peerAddress).getStream().output().send(new StreamDataEvent<>(message));

                //we only want to perform actions such as event propagation for each message type, not each part of a message if it's broken down
                if (message.getMessageType().equals(BitcoinMsg.MESSAGE_TYPE)) {
                    logger.trace(peerAddress, ((BitcoinMsg) message).getBody().getMessageType().toUpperCase() + " Msg sent.");

                    // We propagate this message to the Bus, so other handlers can pick them up if they are subscribed to:
                    // NOTE: These Events related to messages sent might not be necessary, and they add some multi-thread
                    // pressure, so in the future they might be disabled (for noe we need them for some unit tests):
                    Event event = EventFactory.buildOutcomingEvent(peerAddress, (BitcoinMsg<? extends Message>) message);
                    super.eventBus.publish(event);

                /*
                // we also publish a more "general" event, valid for any outcoming message
                super.eventBus.publish(new MsgSentEvent<>(peerAddress, btcMessage));
                */

                    // We update the state per message, not per message block:
                    updateState(0, 1);
                }


            } else logger.trace(peerAddress, " Request to Send Msg Discarded (unknown Peer)");
        }
    }


    /**
     * This message is used to send any message which is in the format |BODY|stream_bytes where the stream is at the end of the message. If in the future
     * we need a message where the stream is in the middle of the message, then we either need to use _stream directly or create a more abstract wrapping function.
     */
    @Override
    public void stream(PeerAddress peerAddress, StreamRequest streamRequest) {
        synchronized (peerAddress.toString().intern()) {
            logger.trace(peerAddress, "Streaming raw bytes to peer: ");

            //If the initial message is greater than 4GB, then we will construct a HeaderEn message and the rest will be appended in batches.
            ByteArrayBuffer byteArrayBuffer = new ByteArrayBuffer();
            Iterator<byte[]> streamItr = streamRequest.getStream().iterator();

            while (streamItr.hasNext() && byteArrayBuffer.size() <= config.getBasicConfig().getThresholdSizeExtMsgs()) {
                byteArrayBuffer.add(streamItr.next());
            }
            ByteStreamMsg initialBodyMsg = new ByteStreamMsg(byteArrayBuffer);

            BitcoinMsg<ByteStreamMsg> initialMessage = new BitcoinMsgBuilder(config.getBasicConfig(), initialBodyMsg)
                    .overrideHeaderMsgType(streamRequest.getMsgType())
                    .overrideHeaderMsgLength(streamRequest.getLen())
                    .build();

            //send the initial message to the peer
            handlerInfo.get(peerAddress).getStream().output().send(new StreamDataEvent<>(initialMessage));

            //if batch size isn't configured for the raw bytes class, then default to 1 GB message sizes
            int batchSizeBytes = config.getMsgBatchConfigs().get(ByteStreamMsg.class) != null ?
                    config.getMsgBatchConfigs().get(ByteStreamMsg.class).getMaxBatchSizeInbytes() : 1_000_000_000;

            //loop the remaining stream and send in chunks of bytes
            ByteArrayBuffer batchByteBuffer = new ByteArrayBuffer();
            while (streamItr.hasNext()) {
                batchByteBuffer.add(streamItr.next());

                //if we've exceeded the maximum size, send it down the wire and start again
                if (batchByteBuffer.size() > batchSizeBytes) {
                    ByteStreamMsg bodyMsgPart = new ByteStreamMsg(batchByteBuffer);
                    handlerInfo.get(peerAddress).getStream().output().send(new StreamDataEvent<>(bodyMsgPart));

                    batchByteBuffer = new ByteArrayBuffer();
                }
            }

            //send the last message if there's any remaining bytes
            if (batchByteBuffer.size() > 0) {
                handlerInfo.get(peerAddress).getStream().output().send(new StreamDataEvent<>(initialMessage));
            }

            logger.trace(peerAddress, streamRequest.getMsgType() + " Msg streamed to peer");

            updateState(0, 1);
        }
    }


    @Override
    public void broadcast(BitcoinMsg<?> btcMessage) {
        handlerInfo.values().forEach(p -> broadcastExecutor.submit(() -> send(p.getStream().getPeerAddress(), btcMessage)));
    }

    @Override
    public void broadcast(BodyMessage msgBody) {
        handlerInfo.values().forEach(p -> broadcastExecutor.submit(() -> send(p.getStream().getPeerAddress(), msgBody)));
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
        if (msg.getLengthInBytes() < msg.getBody().getLengthInBytes()) {
            return "Header is undersized";
        }
        if (msg.getHeader().getMsgLength() > msg.getBody().getLengthInBytes()) {
            return "Header is oversized";
        }

        // Checks the checksum:
        if (config.isVerifyChecksum()
                && msg.getHeader().getMsgLength() > 0
                && msg.getHeader().getChecksum() != msg.getBody().getChecksum()) {
            return "Checksum is Wrong (" + msg.getHeader().getChecksum() + "/" + msg.getBody().getChecksum() + ")";
        }

        // Checks the network specified in magic number:
        if (msg.getHeader().getMagic() != config.getBasicConfig().getMagicPackage()) {
            return "Network Id is incorrect";
        }

        // Checks for 4GB Support:
        if (msg.getLengthInBytes() >= config.getBasicConfig().getThresholdSizeExtMsgs()) {
            if (!msg.getHeader().getCommand().equalsIgnoreCase(HeaderMsg.EXT_COMMAND))
                return "Message Larger than 4GB but wrong Command";
            if (this.config.getBasicConfig().getProtocolVersion() < ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                return "Message Larger than 4GB but we are running a Protocol < 70016";
        }
        return null;
    }

    public MessageHandlerConfig getConfig() {
        return this.config;
    }

    @Override
    public synchronized void updateConfig(HandlerConfig config) {
        if (!(config instanceof MessageHandlerConfig)) {
            throw new RuntimeException("config class is NOT correct for this Handler");
        }
        this.config = (MessageHandlerConfig) config;
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
