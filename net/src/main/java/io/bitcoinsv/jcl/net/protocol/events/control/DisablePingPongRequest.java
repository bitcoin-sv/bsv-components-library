package io.bitcoinsv.jcl.net.protocol.events.control;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:57
 *
 * A Request to Disable the Ping/Pong protocol for a Particular Peer
 */
public final class DisablePingPongRequest extends P2PRequest {
    private final PeerAddress peerAddress;

    public DisablePingPongRequest(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }

    @Override
    public String toString() {
        return "DisablePingPongRequest(peerAddress=" + this.getPeerAddress() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))                 { return false; }
        DisablePingPongRequest other = (DisablePingPongRequest) obj;
        return Objects.equal(this.peerAddress, other.peerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress);
    }
}
