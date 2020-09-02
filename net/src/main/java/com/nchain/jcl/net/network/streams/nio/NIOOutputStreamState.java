package com.nchain.jcl.net.network.streams.nio;

import com.nchain.jcl.base.tools.streams.StreamState;
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
 * This class stores the state of the NIOOutputStreamState.
 */
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class NIOOutputStreamState extends StreamState {
    // Number of bytes sent through this stream since the beginning
    @Builder.Default
    private BigInteger numBytesSent = BigInteger.ZERO;
}
