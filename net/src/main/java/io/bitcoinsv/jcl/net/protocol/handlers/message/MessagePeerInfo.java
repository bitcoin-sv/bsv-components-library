/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.handlers.message;


import io.bitcoinsv.jcl.net.protocol.streams.MessageStream;

/**
 * @author i.fernande@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores info about each Peer we are connected to. For each one, we store the MessageStream
 * that wraps up the communication between that Peer and us.
 */
public final class MessagePeerInfo {
    private final MessageStream stream;

    public MessagePeerInfo(MessageStream stream) {
        this.stream = stream;
    }

    public MessageStream getStream() {
        return this.stream;
    }

    @Override
    public String toString() {
        return "MessagePeerInfo(stream=" + this.getStream() + ")";
    }
}
