package com.nchain.jcl.network.events;

import com.nchain.jcl.network.streams.nio.NIOStream;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-23 10:12
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
