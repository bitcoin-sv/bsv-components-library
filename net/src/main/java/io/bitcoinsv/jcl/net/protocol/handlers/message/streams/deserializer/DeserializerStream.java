package io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer;


import io.bitcoinsv.jcl.net.network.streams.*;
import io.bitcoinsv.jcl.net.network.streams.*;
import io.bitcoinsv.jcl.net.network.streams.nio.NIOInputStream;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessagePreSerializer;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.jcl.net.protocol.serialization.HeaderMsgSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MsgSerializersFactory;
import io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs.MsgPartDeserializationErrorEvent;
import io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs.MsgPartDeserializedEvent;

import io.bitcoinsv.jcl.tools.bytes.ByteArrayBuffer;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayConfig;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.net.tools.LoggerUtil;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An InputStream that takes a ByteArrayReader as input and returns a full Bitcoin Object,
 * performing a De-serialization over the ByteArray before returning..
 *
 * NOTES About the Behaviour of this class:
 *
 * - The main function is the "transform()" function, which is triggered when we receive some bytes. Then it runs
 *   the Deserialization, and returns the result, which will be send down the Stream by the Parent Class.
 *
 * - There is one instance of this class for each Peer we are connected to. All those Streams are using the SAME
 *   Thread, what is called the SHARED Thread.
 *
 * - If the Message is "normal", we usually process it only when we received all its bytes. If we don.t have them,
 *   we just return nothing. Next time we receive more bytes, the cycle will be triggered again, and when we got all
 *   the bytes we need, we deserialize it and return it.
 *
 * - If the Message is "Big", we CANNOT afford to keep all its bytes before we start deserializing it, since we might
 *   run out of Memory. So in these cases, we perform a "Real-Time" Deserialization: We start deserialization from
 *   moment we get the first bytes. At some time we might get to a point when we don't have yet enough bytes to
 *   deserialize (they are still coming), so in that case we WAIT. But this waiting would mean to BLOCK the Thread
 *   which is being used by all the Peers (a SHARED Thread), so that's ot an option.
 *
 *   So the Solution implemented is this:
 *
 *   - The SHARED Thread is always running. It takes new incoming Bytes, feed our buffer with them, and process them.
 *   - If we detect a "Big" Message is coming, then a NEW THREAD is Launched (Called DEDICATED THREAD). From that
 *     moment moving forward, the SHARED Thread will only take care of feeding our buffer with more bytes, and the
 *     DEDICATED Thread wil process them. When the Real-Time Processing ends, the DEDICATED Thread will finish and
 *     we'll get back to the original situation (only the SHARED THREAD).
 *   - A consequence of this is that we cannot notify the parent class about the result of the Deserialization by
 *     the result returned by the "transform" class. So instead of that, the "transform()" function" will also return
 *     NULL, and the Messages (or Errors) will be notified by the methods "processOK()" and "processError()"
 */


public class DeserializerStream extends PeerInputStreamImpl<ByteArrayReader, Message> {

    // State of this Stream. This variable contains all the information about whats going on at any time
    // Along the execution of this Stream this state wil be updated any time we receive new bytes, or we
    // deserialize different arts of the incoming message.
    DeserializerStreamState state; // immutable Class

    // For loggin:
    private LoggerUtil logger;

    // Configuration:
    private RuntimeConfig runtimeConfig;
    private MessageHandlerConfig messageHandlerConfig;

    // We use this ByteArrayBuffer to store the incoming bytes:
    private ByteArrayBuffer buffer;

    // Executor used to trigger real-time deserializers for big Messages:
    private ExecutorService bigMsgsDeserializersExecutor;

    // We can only deserialize "Big" messages if this flag is TRUE, otherwise an Error is thrown.
    // Doing Real-Time processing implies launching a new Thread, so it should be done carefully. So to do it, we
    // need to specifically "allow" the Stream to do so.
    private boolean realTimeProcessingEnabled = false;

    // If set, this object will be triggered BEFORE the Deserialization process...
    private MessagePreSerializer preSerializer;

    // This is the component responsible for deserializing the normal/small messages. It implements an internal CACHE
    //, so if several EQUALS messages are coming down the wire, they wil be taking from the cache instead, which
    // speeds things up a bit.
    // Big Messages are Deserialized the regular way, this class is NOT used for that.

    // This class is STATIC, so there is ONLY ONE CACHE FOR ALL Streams, so all the incoming data from all the
    // remote Peers will be using the same Cache

    private static Deserializer deserializer;

    // If this Stream is closed by the remote Peer, we activate this FLAG:
    private boolean streamClosed = false;


    /** Constructor */
    public DeserializerStream(ExecutorService eventBusExecutor,
                              PeerInputStream<ByteArrayReader> source,
                              RuntimeConfig runtimeConfig,
                              MessageHandlerConfig messageHandlerConfig,
                              Deserializer deserializer,
                              ExecutorService bigMsgsDeserializersExecutor,
                              LoggerUtil parentLogger) {
        super(eventBusExecutor, source);
        this.runtimeConfig = runtimeConfig;
        this.messageHandlerConfig = messageHandlerConfig;
        //this.buffer = new ByteArrayBuffer(runtimeConfig.getByteArrayMemoryConfig());
        this.buffer = new ByteArrayBuffer(deserializer.getConfig().getBufferInitialSizeInBytes(), runtimeConfig.getByteArrayMemoryConfig());
        this.bigMsgsDeserializersExecutor = bigMsgsDeserializersExecutor;

        // We initialize the State:
        this.state = DeserializerStreamState.builder().build();

        // We initialize the Deserializer
        this.deserializer = deserializer;

        // logger:
        this.logger = (parentLogger == null)
                ? new LoggerUtil(this.getPeerAddress().toString(), this.getClass())
                : LoggerUtil.of(parentLogger, "Deserializer", this.getClass());

    }

    /** Constructor */
    public DeserializerStream(ExecutorService eventBusExecutor,
                              PeerInputStream<ByteArrayReader> source,
                              RuntimeConfig runtimeConfig,
                              MessageHandlerConfig messageHandlerConfig,
                              Deserializer deserializer,
                              ExecutorService bigMsgsDeserializersExecutor) {
        this(eventBusExecutor, source, runtimeConfig, messageHandlerConfig, deserializer,
                bigMsgsDeserializersExecutor, new LoggerUtil("Deserializer", DeserializerStream.class));
    }

    @Override
    public void onClose(Consumer<? extends StreamCloseEvent> eventHandler) {
        super.onClose(eventHandler);
        this.streamClosed = true;
    }
    /**
     * It updates the state of this class to reflect that an error has been thrown. The new State is returned.
     */
    private DeserializerStreamState processError(boolean isThisADedicatedThread, Throwable e, DeserializerStreamState state) {
        logger.error(this.peerAddress, "Error Deserializing", e.getMessage(),(streamClosed? "Stream was previously closed" : "Stream still open"));
        if (!streamClosed) {
            logger.error((e.getMessage() != null)? e.getMessage() : e.getCause().getMessage());
            // We notify the parent about this Error and return:
            super.eventBus.publish(new StreamErrorEvent(e));
        }
        return state.toBuilder()
                .processState(DeserializerStreamState.ProcessingBytesState.CORRUPTED)
                .workToDoInBuffer(false)
                .deserializerState(deserializer.getState())
                .build();
    }

    /**
     * It updates the state of this class to reflect that a new Message has been Deserialized. The message is notified
     * to the Stream by direclty invoking the parent, adn the new State is returned.
     */
    private DeserializerStreamState processOK(boolean isThisADedicatedThread, BitcoinMsg<?> message, DeserializerStreamState state) {
        trace(isThisADedicatedThread, message.getBody().getMessageType().toUpperCase() + " Deserialized.");
        //log(isThisADedicatedThread, " Buffer After Deserialization: " + HEX.encode(new ByteArrayReader(buffer).get()));
        // We notify the parent about the new Message Deserialized and return:
        super.eventBus.publish(new StreamDataEvent<>(message));
        // We return the updated State:
        return state.toBuilder()
                .currentBitcoinMsg(message)
                .numMsgs(state.getNumMsgs().add(BigInteger.ONE))
                .deserializerState(deserializer.getState())
                .build();
    }

    /**
     * The MAIN Function of the Deserializer.
     * It is called every time new bytes are received by this Stream (see InputStreamImpl.receiveAndTransform()).
     */
    @Override
    public synchronized List<StreamDataEvent<Message>> transform(StreamDataEvent<ByteArrayReader> dataEvent) {
        try {
            // If some error has occurred already, we don't process any more data...
            if (state.getProcessState().isCorrupted() || dataEvent == null || dataEvent.getData() == null) return null;

            // We feed the buffer with the incoming bytes....
            //log.trace("SHARED Thread :: " + dataEvent.getData().size() + " bytes received, " + buffer.size() + " bytes in buffer. " + Thread.activeCount() + " active Threads...");
            buffer.add(dataEvent.getData().getFullContent());

            // We update the State with the new incoming bytes...
            state = state.toBuilder()
                    .currentMsgBytesReceived(state.getCurrentMsgBytesReceived() + dataEvent.getData().size())
                    .workToDoInBuffer(true)
                    .deserializerState(deserializer.getState())
                    .build();

            // Now we process the Bytes. If there is ONLY one Thread running (the SHARED Thread), we process them. But if
            // there is already a DEDICATED Thread processing the bytes, then we do nothing (since the DEDICATED Thread is
            // already running, and it will process those bytes)
            if (!state.getTreadState().dedicatedThreadRunning()) processBytes(false, state);

        } catch (Throwable e) {
            e.printStackTrace();
        }
        // always return NULL. The parent will be notified directly through the "processOK()" and "processError()" methods
        return null;
    }

    /**
     * This method is called when the next Bytes in the buffer belong to the BODY of a message.
     * This method deserializes them and notifies the parent about either the result or any error thrown.
     *
     * @param isThisADedicatedThread if TRUE, we are running in the DEDICATED Thread, otherwise this is the SHARED Thread.
     * @param realTime               Determines how to perform the Deserialization
     * @param state                  Current State of this class
     * @param buffer                 our Buffer of bytes
     * @return                       the state of this class, updated.
     */
    private DeserializerStreamState deserialize(boolean isThisADedicatedThread, boolean realTime, DeserializerStreamState state, ByteArrayBuffer buffer) {

        try {

            DeserializerStreamState.DeserializerStreamStateBuilder resultBuilder = state.toBuilder();

            // We deserialize the Body of the Message. At this moment we don't know if all the bytes for the
            // body have been received or not, but we dont really care:
            // - If "realTime" is FALSE, then the Deserialization will fail unless all the Bytes are there.
            // - If "realTime" is TRUE, then the Deserialization will be carried out in real time, so all the bytes do
            //   NOT have to be there, they will be consumed as they arrive

            HeaderMsg headerMsg = state.getCurrentHeaderMsg();
            DeserializerContext desContext = DeserializerContext.builder()
                    .protocolBasicConfig(messageHandlerConfig.getBasicConfig())
                    .maxBytesToRead(headerMsg.getMsgLength())
                    .insideVersionMsg(headerMsg.getMsgCommand().equalsIgnoreCase(VersionMsg.MESSAGE_TYPE))
                    .calculateChecksum( messageHandlerConfig.isVerifyChecksum())
                    .build();

            // We instantiate a ByteArrayReader that will be used to read the bytes from the buffer during deserialization
            // NOTE: Each specific Deserializer might wrap this reader with another one, like the ByteArrayReaderOptimized
            // or the ByteArrayReaderRealTime. That depends on the Deserializer implementation.
            //ByteArrayReader byteReader = new ByteArrayReaderOptimized(buffer);
            ByteArrayReader byteReader = new ByteArrayReader(buffer);

            // Here comes the Deserialization: This process is blocking, the only difference between "REAl-TIME" or
            // not is that, in REAL-TIME, the Deserialization can "wait" until the bytes arrive, and the bytes are consumed
            // as they come. In "normal" mode, all the bytes are already there, so the deserialization is performed
            // right away.

            // A HACK Here: We update the Global State, to reflect we are in DESERIALIZING State:
            this.state = state.toBuilder().processState(DeserializerStreamState.ProcessingBytesState.DESERIALIZING_BODY).build();

            // If some error has been triggered during Deserialization in Real-Time, this will be true:
            AtomicBoolean errorRTDeserialization = new AtomicBoolean();

            // If the Deserialization goes well, this will store the State after that.
            AtomicReference<DeserializerStreamState> stateAfterOK = new AtomicReference<>();

            // We use the Deserializer. Depending on "realTime", we use a different method:
            // if "realTime = TRUE", then we assume it's a LARGE message, so we need to define the callbacks that will be
            // populated by the deserializer while doing its work
            // if "realTime = FALSE", then its a small message.

            if (realTime) {
                // We define the Callbacks:
                Consumer<MsgPartDeserializationErrorEvent> onErrorHandler = e -> {
                    this.processError(isThisADedicatedThread, e.getException(), state);
                    errorRTDeserialization.set(true);
                };

                Consumer<MsgPartDeserializedEvent> onPartDeserializedHandler = e -> {
                    // We are notified about a Partial Msg being deserialized. We create the BitcoinMsg and we notify it:
                    Message partialMessage = (Message) e.getData();
                    HeaderMsg partialMsgHeader = HeaderMsg.builder()
                            .magic(headerMsg.getMagic())
                            .command(partialMessage.getMessageType())
                            .length(partialMessage.getLengthInBytes())
                            // Checksum is ZERO for Partial Messages:
                            .checksum(0)
                            // the "extXXX" fields are used for Messages bigger than 4GB (after 70016), but the Partial
                            // messages returned by the Large Serializers are smaller than that, so we set empty values
                            .extCommand(null)
                            .extLength(0)
                            .build();
                    BitcoinMsg<?> bitcoinMsg = new BitcoinMsg(partialMsgHeader, (BodyMessage) e.getData());
                    DeserializerStreamState stateResult = this.processOK(isThisADedicatedThread, bitcoinMsg, state);
                    stateAfterOK.set(stateResult);
                };
                // And then we call the Deserializer...
                deserializer.deserializeLarge(headerMsg, desContext, byteReader, onErrorHandler, onPartDeserializedHandler);

            } else {
                // This a normal (not-realTime) Deserialization. Art this moment, we also triggered a BytesReceivedEvent,
                // if enabled...
                // If the Pre-Serializer is set, we trigger it now...
                if (preSerializer != null) {
                    // We deserialize the bytes from the header:
                    ByteArrayWriter writer = new ByteArrayWriter();
                    HeaderMsgSerializer.getInstance().serialize(null, headerMsg, writer);
                    byte[] headerBytes = writer.reader().getFullContentAndClose();
                    // We deserialize the body Bytes:
                    byte[] bodyBytes = byteReader.get((int)headerMsg.getMsgLength());
                    // we put them together and we launch the Pre-Serializer...
                    byte[] completeMsg = new byte[headerBytes.length + bodyBytes.length];
                    System.arraycopy(headerBytes, 0, completeMsg, 0, headerBytes.length);
                    System.arraycopy(bodyBytes, 0, completeMsg, headerBytes.length, bodyBytes.length);
                    preSerializer.processBeforeDeserialize(getPeerAddress(), headerMsg, completeMsg);
                }
                // The whole message is deserialized
                //System.out.println("Deserializing regular message, length: " + headerMsg);
                BodyMessage bodyMsg = deserializer.deserialize(headerMsg, desContext, byteReader);
                BitcoinMsg<?> bitcoinMsg = new BitcoinMsg<>(headerMsg, bodyMsg);
                // We notify it...
                DeserializerStreamState stateResult = this.processOK(isThisADedicatedThread, bitcoinMsg, state);
                stateAfterOK.set(stateResult);
            }


            // If an Error has been triggered, then we do not process any more. Otherwise, we only keep processing bytes
            //( if there area actually some left in the buffer..
            if (errorRTDeserialization.get()) {
                resultBuilder.processState(DeserializerStreamState.ProcessingBytesState.CORRUPTED);
                resultBuilder.workToDoInBuffer(false);
            } else {
                // We update the State Builder:
                resultBuilder.currentBodyMsg(stateAfterOK.get().getCurrentBodyMsg());
                resultBuilder.currentBitcoinMsg(stateAfterOK.get().getCurrentBitcoinMsg());
                resultBuilder.numMsgs(stateAfterOK.get().getNumMsgs());
                resultBuilder.processState(DeserializerStreamState.ProcessingBytesState.SEEKING_HEAD);
                resultBuilder.workToDoInBuffer(byteReader.size() > 0);
            }

            // The Deserialization is done, so the counter of bytes belonging to the next MSg is reset...
            resultBuilder.currentMsgBytesReceived(0);

            // We return the updated State:
            return resultBuilder.build();

        } catch (Exception e) {
            logger.error("Error Deserializing from " + this.peerAddress, e);
            if (!streamClosed) { e.printStackTrace();}
            return processError(isThisADedicatedThread, e, state);
        }
    }

    // It check is the content of the buffer contains a complete header.
    // The length of a Header might be different depending on whether its a REGULAR header (24 bytes) or an
    // EXTENDED one (44 bytes). And the type of the header depends on the COMMAND field

    private boolean isIncomingHeaderInBufferAlready(ByteArrayBuffer buffer) {
        boolean result = false;
        // If the number of bytes is 44, then it contains a HEADER for sure:
        if (buffer.size() >= HeaderMsg.MESSAGE_LENGTH_EXT) {
            result = true;
        } else if (buffer.size() >= 16){ // at least we have the first 2 fields: magic(4) + command(12)
            try {
                byte[] commandBytes = Arrays.copyOfRange(buffer.get(16), 4, 15);
                String command = new String(commandBytes, "UTF-8");
                result = (!command.equalsIgnoreCase(HeaderMsg.EXT_COMMAND) && (buffer.size() >= HeaderMsg.MESSAGE_LENGTH));
            } catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
        }
        return result;
    }

    /**
     * This method assumes that we are in the process of receiving a new Message HEADER. It will check if we already
     * have those bytes in our buffer and deserializes them if so.
     *
     * @param isThisADedicatedThread if TRUE, we are running in the DEDICATED Thread, otherwise this is the SHARED Thread.
     * @param state                  Current State of this class
     * @param buffer                 our Buffer of bytes
     * @return                       the state of this class, updated.
     */

    private DeserializerStreamState processSeekingHead(boolean isThisADedicatedThread, DeserializerStreamState state, ByteArrayBuffer buffer) {
        DeserializerStreamState.DeserializerStreamStateBuilder result = state.toBuilder();

        //System.out.println(" TRACE:: " + this.peerAddress + " >> " + buffer.size() + " bytes in buffer, Still looking for HEADER...");

        // If the full header is not here yet, we wait...
        if (!isIncomingHeaderInBufferAlready(buffer)) {
            trace(isThisADedicatedThread, "Seeking Header :: Waiting for more Bytes...");
            result.workToDoInBuffer(false);
        }
        else {
            // We deserialize the Header:
            trace(isThisADedicatedThread, "Seeking Header :: Deserializing Header...");

            DeserializerContext desContext = DeserializerContext.builder()
                    .protocolBasicConfig(this.messageHandlerConfig.getBasicConfig())
                    .insideVersionMsg(false)
                    .build();
            ByteArrayReader byteReader = new ByteArrayReader(buffer);
            //log(isThisADedicatedThread, "Reading Header : " + HEX.encode(byteReader.get()));
            HeaderMsg headerMsg = HeaderMsgSerializer.getInstance().deserialize(desContext, byteReader);

            // Now we need to figure out if this incoming Message is one we need to Deserialize, or just Ignore, and that
            // depends on whether we have a Serializer Implementation for it...
            boolean doWeNeedRealTimeProcessing = headerMsg.getMsgLength() >= runtimeConfig.getMsgSizeInBytesForRealTimeProcessing();
            boolean ignoreMsg = !MsgSerializersFactory.hasSerializerFor(headerMsg.getMsgCommand(), doWeNeedRealTimeProcessing);

            // The Header has been processed. After the HEAD a BODY must ALWAYS come, so there is still work todo...
            boolean stillWorkToDoInBuffer = true;

            // Depending on the Size of the incoming BODY, we upgrade the Buffer or not...
            if (doWeNeedRealTimeProcessing)
                    buffer.updateConfig(new ByteArrayConfig(ByteArrayConfig.ARRAY_SIZE_BIG));
            else    buffer.updateConfig(new ByteArrayConfig(ByteArrayConfig.ARRAY_SIZE_NORMAL));

            // We update the State:
            result.currentHeaderMsg(headerMsg)
                    .processState((ignoreMsg)
                            ? DeserializerStreamState.ProcessingBytesState.IGNORING_BODY
                            : DeserializerStreamState.ProcessingBytesState.SEEIKING_BODY)
                    .currentBodyMsg(null)
                    .workToDoInBuffer(stillWorkToDoInBuffer);

            // If the next Step is TO IGNORE the incoming Body, we initialize the variable that will help us keep track of
            // how many bytes we still need to ignore (they might come in different batches)
            if (ignoreMsg) {
                this.logger.warm(this.peerAddress, "No Deserializer found for msg " + headerMsg.getMsgCommand().toUpperCase() + ". Ignoring msg...");
                trace(isThisADedicatedThread, "Ignoring BODY for " + headerMsg.getMsgCommand() + "...");
                result.reminingBytestoIgnore(headerMsg.getMsgLength());
            } else {
                trace(isThisADedicatedThread, "Header Deserialized, now expecting a BODY for " + headerMsg.getMsgCommand().toUpperCase() + "...");
            }
        }
        return result.build();
    }

    /**
     * This method assumes that we are in the process of receiving a new Message BODY. It will check if we already
     * have those bytes in our buffer and deserializes them if so.
     *
     * @param isThisADedicatedThread if TRUE, we are running in the DEDICATED Thread, otherwise this is the SHARED Thread.
     * @param state                  Current State of this class
     * @param buffer                 our Buffer of bytes
     * @return                       the state of this class, updated.
     */
    private DeserializerStreamState processSeekingBody(boolean isThisADedicatedThread, DeserializerStreamState state, ByteArrayBuffer buffer) {
        DeserializerStreamState.DeserializerStreamStateBuilder result = state.toBuilder();


        // We are Seeking a Body: We have different Scenarios:
        // - The Message is "small/normal": We wait until we have all the bytes from its body before Deserializing:
        // - The Message is "big": The ony way to process a "Big" Message is by using "Real-Time" Deserialization. So we
        //   apply this logic:
        //      - If we are in the SHARED Thread, then we launch a DEDICATED Thread, which will take care of processing
        //        the bytes. Right after that we finish, and this SHARED Thread will do nothing in the future but only
        //        receiving new bytes and feeding them into the buffer.
        //      - If we already are in the DEDICATED Thread, we need to process it in Real-Time.

        // The following variables will control what to do next:

        HeaderMsg currentHeaderMsg              = state.getCurrentHeaderMsg();
        String msgType                          = currentHeaderMsg.getMsgCommand().toUpperCase();
        long bodySize                           = currentHeaderMsg.getMsgLength();
        long bufferSize                         = buffer.size();
        boolean isABigMessage                   = (bodySize > runtimeConfig.getMsgSizeInBytesForRealTimeProcessing());
        boolean allBytesMessageReceived         = (bufferSize >= currentHeaderMsg.getMsgLength());


        // If it's a Big Msg but we are not Allowed to do real-time processing, that's an error...
        if (isABigMessage && !realTimeProcessingEnabled) {
            logger.warm(this.peerAddress, "Big Message (" + msgType + " received, but this Stream is NOT allowed to process");
            return processError(isThisADedicatedThread,
                    new RuntimeException("Big Message Received (" + msgType + ") but Not allowed to Process"),
                    state);
        }

        if (!isABigMessage) {
            if (allBytesMessageReceived) {
                trace(isThisADedicatedThread,  "Seeking Body for " + msgType + " :: Deserializing " + currentHeaderMsg.getMsgCommand() + "...");
                result = deserialize(isThisADedicatedThread,false, state, buffer).toBuilder();

            } else {
                trace(isThisADedicatedThread,  "Seeking Body for " + msgType + " :: " + buffer.size() + " bytes received, waiting for " + (currentHeaderMsg.getMsgLength() - bufferSize) + " more...");
                result.workToDoInBuffer(buffer.size() >= currentHeaderMsg.getMsgLength());
            }
        } else {
            if (isThisADedicatedThread) {
                trace(isThisADedicatedThread,  "Seeking Body for " + msgType + " :: Deserializing " + currentHeaderMsg.getMsgCommand() + " in REAL-TIME...");
                result = deserialize(isThisADedicatedThread, true, state, buffer).toBuilder();

            } else {
                trace(isThisADedicatedThread, "Seeking Body for " + msgType + " :: Launching a DEDICATED Thread...");
                DeserializerStreamState threadState = state.toBuilder()
                        .treadState(DeserializerStreamState.ThreadState.DEDICATED_THREAD)
                        .build();
                try {
                    bigMsgsDeserializersExecutor.submit(() -> this.processBytes(true, threadState));
                    result.treadState(DeserializerStreamState.ThreadState.DEDICATED_THREAD);
                    result.workToDoInBuffer(false);
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        return result.build();
    }

    /**
     * This method assumes that we are in the process of Ignoring the next Message BODY. It will check if we already
     * have those bytes in our buffer and discard them if so.
     *
     * @param isThisADedicatedThread if TRUE, we are running in the DEDICATED Thread, otherwise this is the SHARED Thread.
     * @param state                  Current State of this class
     * @param buffer                 our Buffer of bytes
     * @return                       the state of this class, updated.
     */
    private DeserializerStreamState processIgnoringBody(boolean isThisADedicatedThread, DeserializerStreamState state, ByteArrayBuffer buffer) {
        DeserializerStreamState.DeserializerStreamStateBuilder result = state.toBuilder();

        long remainingBytesToIgnore = state.getReminingBytestoIgnore();
        if (remainingBytesToIgnore > 0) {
            int bytesToRemove = (int) Math.min(remainingBytesToIgnore, buffer.size());
            remainingBytesToIgnore -= bytesToRemove;
            buffer.extract(bytesToRemove);
            trace(isThisADedicatedThread,  "Ignoring Body :: Discarding " + bytesToRemove + " bytes, " + remainingBytesToIgnore + " bytes still to discard...");
        }
        result.reminingBytestoIgnore(remainingBytesToIgnore);
        if (remainingBytesToIgnore == 0) {
            result.processState(DeserializerStreamState.ProcessingBytesState.SEEKING_HEAD);
            result.currentMsgBytesReceived(0);
        }
        result.workToDoInBuffer(buffer.size() > 0);
        return result.build();
    }

    /**
     * This method processes the bytes in our buffer, according to our internal State.
     * the messages from the network can be coming in any order, but we know that the Header comes first, followed
     * by the BODY. So we keep track of which part we are expecting next, and we deserialize it
     *
     * @param isThisADedicatedThread if TRUE, we are running in the DEDICATED Thread, otherwise this is the SHARED Thread.
     * @param state                  Current State of this class
     */
    private void processBytes(boolean isThisADedicatedThread, DeserializerStreamState state) {
        try {

            //System.out.println(" TRACE:: " + this.peerAddress + " >> processing bytes, state: " + state.getProcessState() + "...");
            // If the State is CORRUPTED, we do thing...
            if (state.getProcessState().isCorrupted()) return;

             // If we reach this far, it's because:
            // - there is only the "Shared Thread" running, and we are in it
            // - there is a DEDICATED Thread working, and we are in it

            while (state.isWorkToDoInBuffer()) {
                //log.trace("in the switch...");
                switch(state.getProcessState()) {
                    case SEEKING_HEAD: {
                        state = processSeekingHead(isThisADedicatedThread, state, buffer);
                        break;
                    }
                    case SEEIKING_BODY: {
                        state = processSeekingBody(isThisADedicatedThread, state, buffer);
                        break;
                    }
                    case IGNORING_BODY: {
                        state = processIgnoringBody(isThisADedicatedThread, state, buffer);
                        break;
                    }
                } // switch...

                this.state = state; // updating the global State of this Stream...
                //log.trace("Finishing loop, state = " + state.getProcessState());
            } // while moreDataToProcess...

            if (isThisADedicatedThread) {
                trace(isThisADedicatedThread, "Thread finished.");
                this.state = state.toBuilder().treadState(DeserializerStreamState.ThreadState.SHARED_THREAD).build();
            }

        } catch (Throwable th) {
            if (!streamClosed ) {th.printStackTrace();}
            this.state = processError(isThisADedicatedThread, th, state);
        }
    }


    public void upgradeBufferSize() {
        this.realTimeProcessingEnabled = true;
        ((NIOInputStream) super.source).upgradeBufferSize();
    }

    public void resetBufferSize() {
        this.realTimeProcessingEnabled = false;
        ((NIOInputStream) super.source).resetBufferSize();
    }

    // for convenience...
    private void trace(boolean isThisADedicatedThread, String msg) {
        String threadInfo = ((isThisADedicatedThread) ? "DEDICATED Thread " : "SHARED Thread");
        logger.trace(this.peerAddress, msg, threadInfo);
    }

    public DeserializerStreamState getState() {
        return this.state;
    }

    public void setRealTimeProcessingEnabled(boolean realTimeProcessingEnabled) {
        this.realTimeProcessingEnabled = realTimeProcessingEnabled;
    }

    public void setPreSerializer(MessagePreSerializer preSerializer) {
        this.preSerializer = preSerializer;
    }
}
