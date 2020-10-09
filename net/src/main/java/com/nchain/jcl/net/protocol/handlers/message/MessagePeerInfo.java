package com.nchain.jcl.net.protocol.handlers.message;

import com.nchain.jcl.net.protocol.streams.MessageStream;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author i.fernande@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores info about each Peer we are connected to. For each one, we store the MessageStream
 * that wraps up the communication between that Peer and us.
 */
@Value
@AllArgsConstructor
public class MessagePeerInfo {
    private MessageStream stream;
}
