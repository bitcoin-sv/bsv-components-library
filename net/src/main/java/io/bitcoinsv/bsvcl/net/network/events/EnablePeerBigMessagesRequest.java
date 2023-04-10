package io.bitcoinsv.bsvcl.net.network.events;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to upgrade the connection to a remote Peer, so this connection will be
 * able to deserialize BigMessages from that peer from this moment (otherwise all the BigMesssages coming from
 * that Peer will be discarded and the connection dropped)
 */
public final class EnablePeerBigMessagesRequest extends P2PRequest {
    private PeerAddress peerAddress;
    /** Constructor */
    public EnablePeerBigMessagesRequest(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    @Override
    public String toString() {
        return "ResumeConnectingRequest( peerAddress=" + this.peerAddress + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        EnablePeerBigMessagesRequest other = (EnablePeerBigMessagesRequest) obj;
        return Objects.equal(this.peerAddress, other.peerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress);
    }
}
