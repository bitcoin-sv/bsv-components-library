package com.nchain.jcl.net.protocol.streams;


import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStreamState;
import com.nchain.jcl.net.protocol.streams.serializer.SerializerStreamState;
import com.nchain.jcl.tools.streams.StreamState;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the State of a MessageStream. It's just a placeholder for the States of both the
 * Input and the Output of a MessageStream.
 */
@Value
@Builder(toBuilder = true)
public class MessageStreamState extends StreamState {
    private DeserializerStreamState inputState;
    private SerializerStreamState outputState;
}
