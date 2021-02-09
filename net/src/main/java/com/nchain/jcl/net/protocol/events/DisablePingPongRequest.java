package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:57
 *
 * A Request to Disable the Ping/Pong protocol for a Particular Peer
 */
public final class DisablePingPongRequest extends Event {
    private final PeerAddress peerAddress;

    public DisablePingPongRequest(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }

    @Override
    public String toString() {
        return "DisablePingPongRequest(peerAddress=" + this.getPeerAddress() + ")";
    }
}
