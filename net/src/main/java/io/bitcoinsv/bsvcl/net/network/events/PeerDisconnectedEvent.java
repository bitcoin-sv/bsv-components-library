package io.bitcoinsv.bsvcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is disconnected.
 */
public final class PeerDisconnectedEvent extends P2PEvent {

    /** Definition of Reason why a Peer has been disconnected */
    public enum DisconnectedReason {
        UNDEFINED,                              // undefined in general
        DISCONNECTED_BY_LOCAL,                  // generic reason for getting disconnected by Local (JCL)
        DISCONNECTED_BY_LOCAL_BLACKLIST,        // Peer has been blacklisted
        DISCONNECTED_BY_LOCAL_LAZY_DOWNLOAD,    // Remote block doesn't respond to block download requests
        DISCONNECTED_BY_REMOTE                  // generic reason for getting disconnected by the Remote Peer
    }

    private final PeerAddress peerAddress;
    private final DisconnectedReason reason;

    public PeerDisconnectedEvent(PeerAddress peerAddress, DisconnectedReason reason) {
        this.peerAddress = peerAddress;
        this.reason = reason;
    }

    public PeerAddress getPeerAddress()     { return this.peerAddress; }
    public DisconnectedReason getReason()   { return this.reason; }

    @Override
    public String toString() {
        return "Event[Peer Disconnected]: " + peerAddress.toString() + ": " + reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeerDisconnectedEvent other = (PeerDisconnectedEvent) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.reason, other.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, reason);
    }
}
