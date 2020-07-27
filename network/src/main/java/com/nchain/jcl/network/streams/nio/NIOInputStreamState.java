package com.nchain.jcl.network.streams.nio;

import com.nchain.jcl.tools.streams.StreamState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 13:23
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
