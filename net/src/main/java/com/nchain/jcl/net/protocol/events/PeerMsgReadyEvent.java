package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.net.protocol.streams.MessageStream;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is connected, and the connection is wrapped up in a Message Stream, which
 * will take care of Serializing and Deserializing the Messages coming through it.
 */
public final class PeerMsgReadyEvent extends Event {
    private final MessageStream stream;

    public PeerMsgReadyEvent(MessageStream stream) {
        this.stream = stream;
    }

    public MessageStream getStream() { return this.stream; }

    @Override
    public String toString() {
        return "Event[PeerMsgStream Connected]: " + stream.getPeerAddress().toString();
    }
}
