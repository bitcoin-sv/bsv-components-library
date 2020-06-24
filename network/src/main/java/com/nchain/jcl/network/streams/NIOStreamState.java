package com.nchain.jcl.network.streams;

import com.nchain.jcl.tools.streams.StreamState;
import lombok.Builder;
import lombok.Value;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 15:45
 */
@Value
@Builder(toBuilder = true)
public class NIOStreamState extends StreamState {
    private NIOInputStreamState inputState;
    private NIOOutputStreamState outputState;
}
