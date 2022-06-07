package io.bitcoinsv.jcl.net.protocol.events.control;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PEvent;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer that was currently handshaked, disconnects
 */
public final class PeerHandshakedDisconnectedEvent extends P2PEvent {
    private final PeerAddress peerAddress;
    private final VersionMsg versionMsg;

    public PeerHandshakedDisconnectedEvent(PeerAddress peerAddress, VersionMsg versionMsg) {
        this.peerAddress = peerAddress;
        this.versionMsg = versionMsg;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public VersionMsg getVersionMsg()   { return this.versionMsg; }

    @Override
    public String toString() {
        return "PeerHandshakedDisconnectedEvent(peerAddress=" + this.getPeerAddress() + ", versionMsg=" + this.getVersionMsg() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeerHandshakedDisconnectedEvent other = (PeerHandshakedDisconnectedEvent) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.versionMsg, other.versionMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, versionMsg);
    }
}
