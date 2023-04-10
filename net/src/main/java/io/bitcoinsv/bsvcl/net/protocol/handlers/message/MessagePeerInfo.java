package io.bitcoinsv.bsvcl.net.protocol.handlers.message;


import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.MessageStream;

/**
 * @author i.fernande@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores info about each Peer we are connected to. For each one, we store the MessageStream
 * that wraps up the communication between that Peer and us.
 */
public final class MessagePeerInfo {
    // Ref to the Socket Stream assigned to this Peer:
    private final MessageStream stream;

    // We keep a flag, so we only send/broadcast to those Peers that are handshaked
    private boolean isHandshaked;

    public MessagePeerInfo(MessageStream stream) {
        this.stream = stream;
    }

    public MessageStream getStream()    { return this.stream; }
    public void handshake()             { this.isHandshaked = true;}
    public boolean isHandshaked()       { return this.isHandshaked;}

    @Override
    public String toString() {
        return "MessagePeerInfo(stream=" + this.getStream() + ")";
    }
}
