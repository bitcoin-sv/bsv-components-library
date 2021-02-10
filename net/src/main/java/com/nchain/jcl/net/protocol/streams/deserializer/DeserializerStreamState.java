package com.nchain.jcl.net.protocol.streams.deserializer;


import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.tools.streams.StreamState;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the current State of a Deserializer Stream.
 */
public class DeserializerStreamState extends StreamState {

    /**
     * State of the Deserialization process. The bytes form a Message might not come at the same time, so every time
     * we receive new bytes, we need to keep track of the state we are and what to expect next.
     */
    public enum ProcessingBytesState {
        SEEKING_HEAD,               // we are downloading bytes to get a Message Header
        SEEIKING_BODY,              // we are downloading bytes to the a Message Body (payload)
        DESERIALIZING_BODY,         // we are deserializing the Body in Real-Time
        IGNORING_BODY,              // we are ignoring the body
        CORRUPTED;                  // the data received do not make any sense. The Stream will be closed soon

        boolean isCorrupted() { return this == CORRUPTED;}  // convenience
    }

    /**
     * By Default, the Deserialization process runs in the same Thread as all the other Serialization Streams
     * connected to other Peers. So if we are connected to 30 Peers, all of them are running in the same Thread.
     * This is called the SHARED_THREAD (default).
     *
     * The Header of the Message is only deserialized once we have received all its bytes. But for the Body message,
     * The behaviour depends on the Message Size:
     *
     *  - If the Message is "small", then we WAIT until we receive all the bytes from the Body, and then we deserialize.
     *  - If the Message is "Big", we cannot afford to wait for all the bytes, since we might run out of memory. So in
     *    this case, we deserialize the Message in "real-time", meaning we process the Bytes as soon as they come,
     *    without waiting to get the whole Body first. But this "real-time" processing implies launching a new Dedicated
     *    Thread to do this "real-time" processing.
     *    When a Dedicated thread is launched, it will take care of processing all the incoming Bytes, until no more
     *    bytes come. During that time, the "normal/Shared" Thread will only feed the buffer with the new bytes
     *    coming, but it will not process them (since that work is already being carried out by the DEDICATED Thread)
     *
     *    This class is IMMUTABLE and SAFE for MULTI-THREAD
     */
    public enum ThreadState {
        // There is only 1 thread running (the Shared Thread). This Thread takes care of everythign: receiving the new
        // incoming Bytes, and processing them:
        SHARED_THREAD,
        // There are 2 Threads running: The SHARED Thread will only take care of Receiving the new Bytes, The
        // DEDICATED Thread will process them.
        DEDICATED_THREAD;

        public boolean dedicatedThreadRunning()    { return this == DEDICATED_THREAD;}
    }


    private ProcessingBytesState processState = ProcessingBytesState.SEEKING_HEAD;
    private ThreadState treadState = ThreadState.SHARED_THREAD;

    // Info about the current Message being deserialized:
    private HeaderMsg       currentHeaderMsg;
    private Message         currentBodyMsg;
    private BitcoinMsg<?>   currentBitcoinMsg;
    private long            currentMsgBytesReceived;

    // This variable indicates whether there are still Bytes in the Buffer, and those bytes can be processed Now.
    // sometimes we DO have bytes remaining in the buffer, but we can't process just yet because we are waiting for
    // more to come (like when we deserialize a Header or a small Message). If this variable is TRUE, it means that
    // those bytes can be processed, so no need to wait more...
    private boolean workToDoInBuffer = true;

    // If we are in IGNORING_BODY mode this stores the number of bytes still pending to ignore from the Buffer
    private long reminingBytestoIgnore = 0;

    // Some variables to count the number of messages processed:
    private BigInteger numMsgs = BigInteger.ZERO;

    // State of the Deserializer, including Cache info...
    private DeserializerState deserializerState = DeserializerState.builder().build();


    public DeserializerStreamState(ProcessingBytesState processState, ThreadState treadState, HeaderMsg currentHeaderMsg, Message currentBodyMsg, BitcoinMsg<?> currentBitcoinMsg, Long currentMsgBytesReceived, Boolean workToDoInBuffer, Long reminingBytestoIgnore, BigInteger numMsgs, DeserializerState deserializerState) {
        if (processState != null)               this.processState = processState;
        if (treadState != null)                 this.treadState = treadState;
        this.currentHeaderMsg = currentHeaderMsg;
        this.currentBodyMsg = currentBodyMsg;
        this.currentBitcoinMsg = currentBitcoinMsg;
        if (currentMsgBytesReceived != null)    this.currentMsgBytesReceived = currentMsgBytesReceived;
        if (workToDoInBuffer != null)           this.workToDoInBuffer = workToDoInBuffer;
        if (reminingBytestoIgnore != null)      this.reminingBytestoIgnore = reminingBytestoIgnore;
        if (numMsgs != null)                    this.numMsgs = numMsgs;
        if (deserializerState != null)          this.deserializerState = deserializerState;
    }

    public ProcessingBytesState getProcessState()   { return this.processState; }
    public ThreadState getTreadState()              { return this.treadState; }
    public boolean isWorkToDoInBuffer()             { return this.workToDoInBuffer; }
    public long getReminingBytestoIgnore()          { return this.reminingBytestoIgnore; }
    public BigInteger getNumMsgs()                  { return this.numMsgs; }
    public DeserializerState getDeserializerState() { return this.deserializerState; }
    public HeaderMsg getCurrentHeaderMsg()          { return this.currentHeaderMsg; }
    public Message getCurrentBodyMsg()              { return this.currentBodyMsg; }
    public BitcoinMsg<?> getCurrentBitcoinMsg()     { return this.currentBitcoinMsg; }
    public long getCurrentMsgBytesReceived()        { return this.currentMsgBytesReceived; }

    public DeserializerStreamStateBuilder toBuilder() {
        return new DeserializerStreamStateBuilder().processState(this.processState).treadState(this.treadState).currentHeaderMsg(this.currentHeaderMsg).currentBodyMsg(this.currentBodyMsg).currentBitcoinMsg(this.currentBitcoinMsg).currentMsgBytesReceived(this.currentMsgBytesReceived).workToDoInBuffer(this.workToDoInBuffer).reminingBytestoIgnore(this.reminingBytestoIgnore).numMsgs(this.numMsgs).deserializerState(this.deserializerState);
    }

    public static DeserializerStreamStateBuilder builder() {
        return new DeserializerStreamStateBuilder();
    }

    /**
     * Builder
     */

    public static class DeserializerStreamStateBuilder {
        private ProcessingBytesState processState;
        private ThreadState treadState;
        private HeaderMsg currentHeaderMsg;
        private Message currentBodyMsg;
        private BitcoinMsg<?> currentBitcoinMsg;
        private Long currentMsgBytesReceived;
        private Boolean workToDoInBuffer;
        private Long reminingBytestoIgnore;
        private BigInteger numMsgs;
        private DeserializerState deserializerState;

        DeserializerStreamStateBuilder() {
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder processState(ProcessingBytesState processState) {
            this.processState = processState;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder treadState(ThreadState treadState) {
            this.treadState = treadState;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder currentHeaderMsg(HeaderMsg currentHeaderMsg) {
            this.currentHeaderMsg = currentHeaderMsg;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder currentBodyMsg(Message currentBodyMsg) {
            this.currentBodyMsg = currentBodyMsg;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder currentBitcoinMsg(BitcoinMsg<?> currentBitcoinMsg) {
            this.currentBitcoinMsg = currentBitcoinMsg;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder currentMsgBytesReceived(long currentMsgBytesReceived) {
            this.currentMsgBytesReceived = currentMsgBytesReceived;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder workToDoInBuffer(boolean workToDoInBuffer) {
            this.workToDoInBuffer = workToDoInBuffer;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder reminingBytestoIgnore(long reminingBytestoIgnore) {
            this.reminingBytestoIgnore = reminingBytestoIgnore;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder numMsgs(BigInteger numMsgs) {
            this.numMsgs = numMsgs;
            return this;
        }

        public DeserializerStreamState.DeserializerStreamStateBuilder deserializerState(DeserializerState deserializerState) {
            this.deserializerState = deserializerState;
            return this;
        }

        public DeserializerStreamState build() {
            return new DeserializerStreamState(processState, treadState, currentHeaderMsg, currentBodyMsg, currentBitcoinMsg, currentMsgBytesReceived, workToDoInBuffer, reminingBytestoIgnore, numMsgs, deserializerState);
        }
    }
}
