package com.nchain.jcl.network.streams.nio;

import com.nchain.jcl.tools.streams.StreamState;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.¡fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 15:45
 *
 * This class stores the State of a NIOStream. It's just a placeholder for the States of both the
 * input and the output channels of the Stream.
 */
@Value
@Builder(toBuilder = true)
public class NIOStreamState extends StreamState {
    private NIOInputStreamState inputState;
    private NIOOutputStreamState outputState;
}
