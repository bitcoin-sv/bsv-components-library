package com.nchain.jcl.network.streams;

import com.nchain.jcl.tools.streams.StreamState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 13:23
 */
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class NIOOutputStreamState extends StreamState {
    @Builder.Default
    private BigInteger numBytesSent = BigInteger.ZERO;
}
