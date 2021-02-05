package com.nchain.jcl.net.network.streams.nio;


import com.nchain.jcl.tools.streams.StreamState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the State of a NIOInputStream
 */
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class NIOInputStreamState extends StreamState {
    // Number of bytes received since the beginning
    @Builder.Default
    private BigInteger numBytesReceived = BigInteger.ZERO;
}
