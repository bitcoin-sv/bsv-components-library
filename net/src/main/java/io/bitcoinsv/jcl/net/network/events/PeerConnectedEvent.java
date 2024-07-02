package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2024 nChain Ltd
 *
 * An Event triggered when a Peer is Connected. This is a physical connection (Socket Connection),
 * so the real communication with this Peer has not even started yet. Most probably you will be interested in the
 * PeerHandshakedEvent, which is triggered when a Peer is connected and the handshake is done, so real
 * communication can be performed.
 */
public final class PeerConnectedEvent extends P2PEvent {
    private final PeerAddress peerAddress;
    private final boolean isOutgoing;

    public PeerConnectedEvent(PeerAddress peerAddress, boolean isOutgoing)  {
        this.peerAddress = peerAddress;
        this.isOutgoing = isOutgoing;
    }
    public PeerAddress getPeerAddress()                 { return this.peerAddress; }
    public boolean isOutgoingConnection()               { return this.isOutgoing; }

    @Override
    public String toString() {
        return String.format("Event[Peer Connected]: %s, %s connection",
            peerAddress.toString(), (isOutgoing ? "outgoing" : "incoming"));
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeerConnectedEvent other = (PeerConnectedEvent) obj;
        return Objects.equal(this.peerAddress, other.peerAddress) && this.isOutgoing == other.isOutgoing;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress);
    }
}
