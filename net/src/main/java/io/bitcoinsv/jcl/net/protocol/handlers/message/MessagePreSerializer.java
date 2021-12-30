package io.bitcoinsv.jcl.net.protocol.handlers.message;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A MessagePreSerializer is an object that can be hooked into the Message-Serialization
 * process and it run right BEFORE the Object is Deserialized and AFTER is Serialized
 */
public interface MessagePreSerializer {
    void processBeforeDeserialize(PeerAddress peerAddress, HeaderMsg headerMsg, byte[] msgBytes);
    void processAfterSerialize(PeerAddress peerAddress, HeaderMsg headerMSg, byte[] msgBytes);
}
