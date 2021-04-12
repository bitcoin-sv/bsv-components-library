package com.nchain.jcl.net.network.events;


import com.nchain.jcl.net.network.streams.nio.NIOStream;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is Connected.
 * NOTE: This is a LOW-LEVEL Event, only meant to be used by other classes in this library, not by the client, since
 * it controls how the information flows between the Library and the remote Peer.
 */
public final class PeerNIOStreamConnectedEvent extends P2PEvent {
    private final NIOStream stream;

    public PeerNIOStreamConnectedEvent(NIOStream stream)    { this.stream = stream; }
    public NIOStream getStream()                            { return this.stream; }
    @Override
    public String toString() {
        return "Event[PeerNIOStream Connected]: " + stream.getPeerAddress().toString();
    }
}
