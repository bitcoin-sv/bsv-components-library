package com.nchain.jcl.net.protocol.streams.serializer;

import com.nchain.jcl.base.tools.streams.StreamState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A class storing the State of the SerializerStream
 */
@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class SerializerStreamState extends StreamState {
    // Some variables to count the number of messages processed:
    @Builder.Default
    private BigInteger numMsgs = BigInteger.ZERO;
}
