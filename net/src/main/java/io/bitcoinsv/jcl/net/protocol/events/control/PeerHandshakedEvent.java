package io.bitcoinsv.jcl.net.protocol.events.control;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PEvent;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;

import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when A Peer has been handshaked and it's ready to communicate with.
 */
public final class PeerHandshakedEvent extends P2PEvent {
    private final PeerAddress peerAddress;
    // Version Msg sent by the remote Peer during the Handshake process:
    private final VersionMsg versionMsg;

    public PeerHandshakedEvent(PeerAddress peerAddress, VersionMsg versionMsg) {
        this.peerAddress = peerAddress;
        this.versionMsg = versionMsg;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public VersionMsg getVersionMsg()   { return this.versionMsg; }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Event[Peer Handshaked]: " + peerAddress + " : " + versionMsg.getUser_agent().getStr());
        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeerHandshakedEvent other = (PeerHandshakedEvent) obj;
        return Objects.equals(this.peerAddress, other.peerAddress)
            && Objects.equals(this.versionMsg, other.versionMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), peerAddress, versionMsg);
    }
}
