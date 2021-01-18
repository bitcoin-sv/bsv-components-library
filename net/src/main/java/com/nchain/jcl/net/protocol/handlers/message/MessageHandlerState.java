package com.nchain.jcl.net.protocol.handlers.message;

import com.nchain.jcl.base.tools.handlers.HandlerState;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

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
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class MessageHandlerState extends HandlerState {
    @Builder.Default
    private BigInteger numMsgsIn = BigInteger.ZERO;
    @Builder.Default
    private BigInteger numMsgsOut = BigInteger.ZERO;

    /** State of the Deserializer Cache (if disabled, all value are ZERO) */
    private DeserializerState deserializerState;

    @Override
    public String toString() {
        String result = "Message Handler State: " + numMsgsIn + " Msgs in, " + numMsgsOut + " Msgs out";
        if (deserializerState == null) result += ". Deserializer Cache Stats Disabled.";
        else result += ". Deserializer Cache Stats: " + deserializerState.toString();
        return result;
    }
}
