package com.nchain.jcl.protocol.streams;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.streams.PeerInputStream;
import com.nchain.jcl.network.streams.PeerStreamInfo;
import com.nchain.jcl.network.streams.nio.NIOInputStreamSource;
import com.nchain.jcl.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.protocol.handlers.message.MessagePreSerializer;
import com.nchain.jcl.protocol.messages.PartialBlockHeaderMsg;
import com.nchain.jcl.protocol.messages.PartialBlockTXsMsg;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.protocol.messages.HeaderMsg;
import com.nchain.jcl.protocol.messages.VersionMsg;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.protocol.messages.common.Message;
import com.nchain.jcl.protocol.serialization.HeaderMsgSerializer;
import com.nchain.jcl.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.protocol.serialization.common.MsgSerializersFactory;
import com.nchain.jcl.protocol.serialization.largeMsgs.LargeMessageDeserializer;
import com.nchain.jcl.tools.bytes.ByteArrayBuilder;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayReaderOptimized;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.streams.InputStream;
import com.nchain.jcl.tools.streams.InputStreamImpl;
import com.nchain.jcl.tools.streams.StreamDataEvent;
import com.nchain.jcl.tools.streams.StreamErrorEvent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-11 09:58
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


public class DeserializerStream extends InputStreamImpl<ByteArrayReader, BitcoinMsg<?>>
                                implements PeerInputStream<BitcoinMsg<?>> {

    // State of this Stream. This variable contains all the information about whats going on at any time
    // Along the execution of this Stream this state wil be updated any time we receive new bytes, or we
    // deserialize different arts of the incoming message.
    @Getter DeserializerStreamState state; // immutable Class

    // For loggin:
    private LoggerUtil logger;

    // Basic attributes
    private RuntimeConfig runtimeConfig;
    private ProtocolBasicConfig protocolBasicConfig;
    private ByteArrayBuilder buffer;
    private ExecutorService realTimeExecutor;

    // We can only deserialize "Big" messages if this flag is TRUE, otherwise an Error is thrown.
    // Doing Real-Time processing implies launching a new Thread, so it should be done carefully. So to do it, we
    // need to specifically "allow" the Stream to do so.
    @Setter
    private boolean realTimeProcessingEnabled = false;

    // If set, this object will be triggered BEFORE the Deserialization process...
    @Setter
    private MessagePreSerializer preSerializer;

    /** Constructor */
    public DeserializerStream(ExecutorService executor, InputStream<ByteArrayReader> source,
                              RuntimeConfig runtimeConfig, ProtocolBasicConfig protocolBasicConfig) {
        super(executor, source);
        this.logger = new LoggerUtil(this.getPeerAddress().toString(), this.getClass());
        this.runtimeConfig = runtimeConfig;
        this.protocolBasicConfig = protocolBasicConfig;
        this.buffer = new ByteArrayBuilder(runtimeConfig.getByteArrayMemoryConfig());
        this.realTimeExecutor = Executors.newSingleThreadExecutor();

        // We initialize the State:
        this.state = DeserializerStreamState.builder().build();
    }

    @Override
    public PeerAddress getPeerAddress() {
        if (source instanceof PeerStreamInfo) return ((PeerStreamInfo) source).getPeerAddress();
        else throw new RuntimeException("The Source of this Stream is NOT connected to a Peer!");
    }


    /**
     * It updates the state of this class to reflect that an error has been thrown. The new State is returned.
     */
    private DeserializerStreamState processError(boolean isThisADedicatedThread, Throwable e, DeserializerStreamState state) {
        logger.error((e.getMessage() != null)? e.getMessage() : e.getCause().getMessage());
        // We notify the parent about this Error and return:
        super.eventBus.publish(new StreamErrorEvent(e));
        return state.toBuilder()
                .processState(DeserializerStreamState.ProcessingBytesState.CORRUPTED)
                .workToDoInBuffer(false)
                .build();
    }

    /**
     * It updates the state of this class to reflect that a new Message has been Deserialized. The message is notified
     * to the Stream by direclty invoking the parent, adn the new State is returned.
     */
    private DeserializerStreamState processOK(boolean isThisADedicatedThread, BitcoinMsg<?> message, DeserializerStreamState state) {
        log(isThisADedicatedThread, message.getBody().getMessageType() + " Deserialized.");
        //log(isThisADedicatedThread, " Buffer After Deserialization: " + HEX.encode(new ByteArrayReader(buffer).getFullContent()));
        // We notify the parent about the new Message Deserialized and return:
        super.eventBus.publish(new StreamDataEvent<>(message));
        // We return the updated State:
        return state.toBuilder()
                .currentBitcoinMsg(message)
                .numMsgs(state.getNumMsgs().add(BigInteger.ONE))
                .build();
    }

    /**
     * The MAIN Function of the Deserializer.
     * It is called every time new bytes are received by this Stream (see InputStreamImpl.receiveAndTransform()).
     */
    @Override
    public synchronized List<StreamDataEvent<BitcoinMsg<?>>> transform(StreamDataEvent<ByteArrayReader> dataEvent) {
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
    private DeserializerStreamState deserialize(boolean isThisADedicatedThread, boolean realTime, DeserializerStreamState state, ByteArrayBuilder buffer) {

        try {

            DeserializerStreamState.DeserializerStreamStateBuilder resultBuilder = state.toBuilder();

            // We deserialize the Body of the Message. At this moment we don't know if all the bytes for the
            // body have been received or not, but we dont really care:
            // - If "realTime" is FALSE, then the Deserialization will fail unless all the Bytes are there.
            // - If "realTime" is TRUE, then the Deserialization will be carried out in real time, so all the bytes do
            //   NOT have to be there, they will be consumed as they arrive

            HeaderMsg headerMsg = state.getCurrentHeaderMsg();
            DeserializerContext desContext = DeserializerContext.builder()
                    .protocolBasicConfig(protocolBasicConfig)
                    .maxBytesToRead(headerMsg.getLength())
                    .insideVersionMsg(headerMsg.getCommand().equalsIgnoreCase(VersionMsg.MESSAGE_TYPE))
                    .build();
            ByteArrayReader byteReader = new ByteArrayReader(buffer);

            // If "Real-time" is enabled, we assume the message is gonna be "Big", so we adjust the Reader:
            if (realTime) {
                byteReader = new ByteArrayReaderOptimized(byteReader);
                byteReader.enableRealTime(runtimeConfig.getMaxWaitingTimeForBytesInRealTime());
            }

            // Here comes the Deserialization: This process is blocking, the only difference between "REAl-TIME" or
            // not is that, in REAL-TIME, the Deserialization can "wait" until the bytes arrive, and the bytes are consumed
            // as they come. In "normal" mode, all the bytes are already there, so the deserialization is performed
            // right away.

            // A HACK Here: We update the Global State, to reflect we are in DESERIALIZING State:
            this.state = state.toBuilder().processState(DeserializerStreamState.ProcessingBytesState.DESERIALIZING_BODY).build();


            // If some error has been triggered during Deserialization in RT, this will be true:
            AtomicBoolean errorRTDeserialization = new AtomicBoolean();

            // If the Deserialization goes well, this will store the State after that.
            AtomicReference<DeserializerStreamState> stateAfterOK = new AtomicReference<>();

            // We locate a Deserializer for this Message:
            // If "realTime" is TRUE, then we assume the Message is be large, so we need a LargeMsgDeserializer,
            // otherwise a MessageSerializer is enough...

            if (realTime) {
                LargeMessageDeserializer largeMsgDeserializer =  MsgSerializersFactory.getLargeMsgDeserializer(headerMsg.getCommand());
                largeMsgDeserializer.onError(e -> {
                    this.processError(isThisADedicatedThread, e.getException(), state);
                    errorRTDeserialization.set(true);
                });
                largeMsgDeserializer.onDeserialized(e -> {
                    // We are notified about a Partial Msg being deserialized. We create the BitcoinMsg and  we notify it
                    HeaderMsg partialMsgHeader = null;
                    if (e.getData() instanceof PartialBlockHeaderMsg)
                        partialMsgHeader = headerMsg.toBuilder().command(PartialBlockHeaderMsg.MESSAGE_TYPE).build();
                    else
                        partialMsgHeader = headerMsg.toBuilder().command(PartialBlockTXsMsg.MESSAGE_TYPE).build();
                    BitcoinMsg<?> bitcoinMsg = new BitcoinMsg(partialMsgHeader, (Message) e.getData());
                    DeserializerStreamState stateResult = this.processOK(isThisADedicatedThread, bitcoinMsg, state);
                    stateAfterOK.set(stateResult);
                });
                largeMsgDeserializer.deserialize(desContext, byteReader);
            } else {
                // This a normal (not-realTime) Deserialization. Art this moment, we also triggered a BytesReceivedEvent,
                // if enabled...
                if (preSerializer != null) {
                    // We get the bytes from the header:
                    ByteArrayWriter writer = new ByteArrayWriter();
                    HeaderMsgSerializer.getInstance().serialize(null, headerMsg, writer);
                    byte[] headerBytes = writer.reader().getFullContentAndClose();
                    // We get the body Bytes:
                    byte[] bodyBytes = byteReader.get((int)headerMsg.getLength());
                    // we put them together and we launch the Pre-Serializer...
                    byte[] completeMsg = new byte[headerBytes.length + bodyBytes.length];
                    System.arraycopy(headerBytes, 0, completeMsg, 0, headerBytes.length);
                    System.arraycopy(bodyBytes, 0, completeMsg, headerBytes.length, bodyBytes.length);
                    preSerializer.processBeforeDeserialize(getPeerAddress(), headerMsg, completeMsg);
                }
                // The whole message is deserialized. We notify it..
                MessageSerializer deserializer = MsgSerializersFactory.getSerializer(headerMsg.getCommand());
                Message bodyMsg = deserializer.deserialize(desContext, byteReader);
                BitcoinMsg<?> bitcoinMsg = new BitcoinMsg<>(headerMsg, bodyMsg);
                DeserializerStreamState stateResult = this.processOK(isThisADedicatedThread, bitcoinMsg, state);
                stateAfterOK.set(stateResult);
            }


            // if we are using an OPTIMIZED  byteArrayReader, we reset it now...
            if (byteReader instanceof ByteArrayReaderOptimized)
                ((ByteArrayReaderOptimized) byteReader).refreshBuffer();

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
            e.printStackTrace();
            return processError(isThisADedicatedThread, e, state);
        }
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

    private DeserializerStreamState processSeekingHead(boolean isThisADedicatedThread, DeserializerStreamState state, ByteArrayBuilder buffer) {
        DeserializerStreamState.DeserializerStreamStateBuilder result = state.toBuilder();

        // If we got all the bytes from the Header, we deserialize it, otherwise we just keep waiting...
        if (buffer.size() < HeaderMsg.MESSAGE_LENGTH) {
            log(isThisADedicatedThread, "Seeking Header :: Waiting for more Bytes...");
            result.workToDoInBuffer(false);
        }
        else {
            // We deserialize the Header:
            log(isThisADedicatedThread, "Seeking Header :: Deserializing Header...");

            DeserializerContext desContext = DeserializerContext.builder()
                    .maxBytesToRead(HeaderMsg.MESSAGE_LENGTH)
                    .insideVersionMsg(false)
                    .build();
            ByteArrayReader byteReader = new ByteArrayReader(buffer);
            //log(isThisADedicatedThread, "Reading Header : " + HEX.encode(byteReader.getFullContent()));
            HeaderMsg headerMsg = HeaderMsgSerializer.getInstance().deserialize(desContext, byteReader);

               // Now we need to figure out if this incoming Message is one we need to Deserialize, or just Ignore, and that
            // depends on whether we have a Serializer Implementation for it...
            boolean doWeNeedRealTimeProcessing = headerMsg.getLength() >= runtimeConfig.getMsgSizeInBytesForRealTimeProcessing();
            boolean ignoreMsg = (doWeNeedRealTimeProcessing)
                    ? MsgSerializersFactory.getLargeMsgDeserializer(headerMsg.getCommand()) == null
                    : MsgSerializersFactory.getSerializer(headerMsg.getCommand()) == null;

            // The Header has been processed. After the HEAD a BODY must ALWAYS come, so there is still work to
            // do...
            boolean stillWorkToDoInBuffer = true;

            // We update the State:
            result.currentHeaderMsg(headerMsg)
                    .processState((ignoreMsg)
                            ? DeserializerStreamState.ProcessingBytesState.IGNORING_BODY
                            : DeserializerStreamState.ProcessingBytesState.SEEIKING_BODY)
                    .currentBodyMsg(null)
                    .workToDoInBuffer(stillWorkToDoInBuffer);

            // If the next Step is TO IGNORE the incoming Body, we initialize the variable that will help us keep track of
            // how many bytes we still need to ignore (they might come in different batches)
            if (ignoreMsg) result.reminingBytestoIgnore(headerMsg.getLength());

            // Some logging:

            if (!ignoreMsg)
                log(isThisADedicatedThread,"Header Deserialized, now expecting a BODY for " + headerMsg.getCommand() + "...");
            else
                log(isThisADedicatedThread,"Ignoring BODY for " + headerMsg.getCommand() + "...");
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
    private DeserializerStreamState processSeekingBody(boolean isThisADedicatedThread, DeserializerStreamState state, ByteArrayBuilder buffer) {
        DeserializerStreamState.DeserializerStreamStateBuilder result = state.toBuilder();


        // We are Seeking a Body: We have different Scenarios:
        // - The Message is "small/normal": We wait until we have all the bytes from its body before Deserializing:
        // - The Message is "big": The ony way to process a "Big" Message is by using "Real-Time" Deserialization. So we
        //   apply this logic:
        //      - If we are in the SHARED Thread, then we launch a DEDICATED Thread, which will take care of processing
        //        the bytes. Right after that we end and this SHARED Thread will do nothing in the future but only
        //        receiving new bytes and feeding them into the buffer.
        //      - If we already are in the DEDICATED Thread, we need to process it in Real-Time.

        // The following variables will control what to do next:

        HeaderMsg currentHeaderMsg              = state.getCurrentHeaderMsg();
        long bodySize                           = currentHeaderMsg.getLength();
        boolean isABigMessage                   = (bodySize > runtimeConfig.getMsgSizeInBytesForRealTimeProcessing());
        boolean allBytesMessageReceived         = (buffer.size() >= currentHeaderMsg.getLength());


        //log(isThisADedicatedThread, "Reading Body : " + HEX.encode(new ByteArrayReader(buffer).getFullContent()));

        // If it's a Big Msg but we are not Allowed to do real-time processing, that's an error...
        if (isABigMessage && !realTimeProcessingEnabled) {
            return processError(isThisADedicatedThread,
                    new RuntimeException("Big Message Received but Not allowed to Process"),
                    state);
        }

        if (!isABigMessage) {
            if (allBytesMessageReceived) {
                log(isThisADedicatedThread, "Seeking Body :: Deserializing " + currentHeaderMsg.getCommand() + "...");
                result = deserialize(isThisADedicatedThread,false, state, buffer).toBuilder();

            } else {
                log(isThisADedicatedThread, "Seeking Body :: Not possible to Deserialize yet...");
                result.workToDoInBuffer(buffer.size() >= currentHeaderMsg.getLength());
            }
        } else {
            if (isThisADedicatedThread) {
                log(isThisADedicatedThread, "Seeking Body :: Deserializing " + currentHeaderMsg.getCommand() + " in REAL-TIME...");
                result = deserialize(isThisADedicatedThread, true, state, buffer).toBuilder();

            } else {
                log(isThisADedicatedThread, "Seeking Body :: Launching a DEDICATED Thread...");
                DeserializerStreamState threadState = state.toBuilder()
                        .treadState(DeserializerStreamState.ThreadState.DEDICATED_THREAD)
                        .build();
                realTimeExecutor.submit(() -> this.processBytes(true, threadState));
                result.treadState(DeserializerStreamState.ThreadState.DEDICATED_THREAD);
                result.workToDoInBuffer(false);
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
    private DeserializerStreamState processIgnoringBody(boolean isThisADedicatedThread, DeserializerStreamState state, ByteArrayBuilder buffer) {
        DeserializerStreamState.DeserializerStreamStateBuilder result = state.toBuilder();

        long remainingBytesToIgnore = state.getReminingBytestoIgnore();
        if (remainingBytesToIgnore > 0) {
            int bytesToRemove = (int) Math.min(remainingBytesToIgnore, buffer.size());
            remainingBytesToIgnore -= bytesToRemove;
            buffer.extractBytes(bytesToRemove);
            log(isThisADedicatedThread, "Ignoring Body :: Discarding " + bytesToRemove + " bytes, " + remainingBytesToIgnore + " bytes still to discard...");
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
                log(isThisADedicatedThread, "Thread finished.");
                this.state = state.toBuilder().treadState(DeserializerStreamState.ThreadState.SHARED_THREAD).build();
            }

        } catch (Throwable th) {
            th.printStackTrace();
            this.state = processError(isThisADedicatedThread, th, state);
        }
    }


    public void upgradeBufferSize() {
        this.realTimeProcessingEnabled = true;
        ((NIOInputStreamSource) super.source).upgradeBufferSize();
    }

    public void resetBufferSize() {
        this.realTimeProcessingEnabled = false;
        ((NIOInputStreamSource) super.source).resetBufferSize();
    }

    // for convenience...
    private void log(boolean isThisADedicatedThread, String msg) {
        String logPreffix = ((isThisADedicatedThread) ? "DEDICATED Thread :: " : "SHARED Thread :: ");
        logger.trace(logPreffix + msg);
    }

}
