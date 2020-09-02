package com.nchain.jcl.net.protocol.handlers.message;

import com.nchain.jcl.net.protocol.streams.MessageStream;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author i.fernande@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 16:06
 *
 * This class stores info about each Peer we are connected to. For each one, we store the MessageStream
 * that wraps up the communication between that Peer and us.
 */
@Value
@AllArgsConstructor
public class MessagePeerInfo {
    private MessageStream stream;
}
