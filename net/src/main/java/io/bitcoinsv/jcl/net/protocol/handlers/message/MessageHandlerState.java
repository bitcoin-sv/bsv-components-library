package io.bitcoinsv.jcl.net.protocol.handlers.message;


import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerState;
import io.bitcoinsv.jcl.tools.handlers.HandlerState;

import java.util.concurrent.atomic.AtomicLong;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * This event stores the state of the Handshake Handler at a point in time.
 * The Message Handler takes care of the Serialization/Deserialization of the information coming
 * from/to the Blockchain P2P Network, converting Bitcoin Messages into bytes (raw data) and the
 * other way around.
 * <p>
 * These events keeps track of the number of bitcoin messages sent to and received from the network.
 */
public final class MessageHandlerState extends HandlerState {
    private AtomicLong numMsgsIn = new AtomicLong();
    private AtomicLong numMsgsOut = new AtomicLong();

    /**
     * State of the Deserializer Cache (if disabled, all value are ZERO)
     */
    private final DeserializerState deserializerState;

    public MessageHandlerState() {
        this.deserializerState = null;
    }

    public MessageHandlerState(long numMsgsIn, long numMsgsOut, DeserializerState deserializerState) {
        this.numMsgsIn.set(numMsgsIn);
        this.numMsgsOut.set(numMsgsOut);
        this.deserializerState = deserializerState;
    }

    @Override
    public String toString() {
        String result = "Message Handler State: " + numMsgsIn + " Msgs in, " + numMsgsOut + " Msgs out.";
        if (deserializerState == null) result += "Deserializer Cache Stats Disabled.";
        else result += "Deserializer Cache Stats: " + deserializerState;

        return result;
    }

    public void incMsgsIn(long num) {
        this.numMsgsIn.addAndGet(num);
    }

    public void incMsgsOut(long num) {
        this.numMsgsOut.addAndGet(num);
    }

    public long getNumMsgsIn() {
        return this.numMsgsIn.get();
    }

    public long getNumMsgsOut() {
        return this.numMsgsOut.get();
    }

    public DeserializerState getDeserializerState() {
        return this.deserializerState;
    }

    public void increaseInMsgCount(int count) {
        numMsgsIn.addAndGet(count);
    }

    public void increaseOutMsgCount(int count) {
        numMsgsIn.addAndGet(count);
    }
}