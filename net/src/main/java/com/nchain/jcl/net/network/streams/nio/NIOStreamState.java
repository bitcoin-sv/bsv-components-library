package com.nchain.jcl.net.network.streams.nio;


import com.nchain.jcl.tools.streams.StreamState;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.¡fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
