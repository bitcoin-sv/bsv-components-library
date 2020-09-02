package com.nchain.jcl.net.protocol.streams;

import com.nchain.jcl.base.tools.streams.StreamState;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 16:16
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
