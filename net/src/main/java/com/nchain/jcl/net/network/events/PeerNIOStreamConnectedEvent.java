package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.streams.nio.NIOStream;
import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is Connected.
 * NOTE: This is a LOW-LEVEL Event, only meant to be used by other classes in this library, not by the client, since
 * it controls how the information flows between the Library and the remote Peer.
 */
@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class PeerNIOStreamConnectedEvent extends Event  {
    private NIOStream stream;

    @Override
    public String toString() {
        return "Event[PeerNIOStream Connected]: " + stream.getPeerAddress().toString();
    }
}
