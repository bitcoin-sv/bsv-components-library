package com.nchain.jcl.net.protocol.handlers.message;



import com.nchain.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerState;
import com.nchain.jcl.tools.handlers.HandlerState;

import java.math.BigInteger;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event stores the state of the Handshake Handler at a point in time.
 * The Message Handler takes care of the Serialization/Deserialization of the information coming
 * from/to the Blockchain P2P Network, converting Bitcoin Messages into bytes (raw data) and the
 * other way around.
 *
 * This events keeps track of the number of bitcoin messages sent to and received from the network.
 */
public final class MessageHandlerState extends HandlerState {
    private BigInteger numMsgsIn = BigInteger.ZERO;
    private BigInteger numMsgsOut = BigInteger.ZERO;

    /** State of the Deserializer Cache (if disabled, all value are ZERO) */
    private final DeserializerState deserializerState;

    public MessageHandlerState(BigInteger numMsgsIn, BigInteger numMsgsOut, DeserializerState deserializerState) {
        if (numMsgsIn != null)  this.numMsgsIn = numMsgsIn;
        if (numMsgsOut != null) this.numMsgsOut = numMsgsOut;
        this.deserializerState = deserializerState;
    }

    @Override
    public String toString() {
        String result = "Message Handler State: " + numMsgsIn + " Msgs in, " + numMsgsOut + " Msgs out";
        if (deserializerState == null) result += ". Deserializer Cache Stats Disabled.";
        else result += ". Deserializer Cache Stats: " + deserializerState.toString();
        return result;
    }

    public BigInteger getNumMsgsIn()                { return this.numMsgsIn; }
    public BigInteger getNumMsgsOut()               { return this.numMsgsOut; }
    public DeserializerState getDeserializerState() { return this.deserializerState; }

    public MessageHandlerStateBuilder toBuilder() {
        return new MessageHandlerStateBuilder().numMsgsIn(this.numMsgsIn).numMsgsOut(this.numMsgsOut).deserializerState(this.deserializerState);
    }

    public static MessageHandlerStateBuilder builder() {
        return new MessageHandlerStateBuilder();
    }

    /**
     * Builder
     */
    public static class MessageHandlerStateBuilder {
        private BigInteger numMsgsIn;
        private BigInteger numMsgsOut;
        private DeserializerState deserializerState;

        MessageHandlerStateBuilder() {}

        public MessageHandlerState.MessageHandlerStateBuilder numMsgsIn(BigInteger numMsgsIn) {
            this.numMsgsIn = numMsgsIn;
            return this;
        }

        public MessageHandlerState.MessageHandlerStateBuilder numMsgsOut(BigInteger numMsgsOut) {
            this.numMsgsOut = numMsgsOut;
            return this;
        }

        public MessageHandlerState.MessageHandlerStateBuilder deserializerState(DeserializerState deserializerState) {
            this.deserializerState = deserializerState;
            return this;
        }

        public MessageHandlerState build() {
            return new MessageHandlerState(numMsgsIn, numMsgsOut, deserializerState);
        }
    }
}
