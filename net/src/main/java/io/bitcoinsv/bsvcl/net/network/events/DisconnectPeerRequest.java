package io.bitcoinsv.bsvcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Disconnect from one Peer. It also might include a Reason for that.
 */
public final class DisconnectPeerRequest extends P2PRequest {

    private final PeerAddress peerAddress;
    private final PeerDisconnectedEvent.DisconnectedReason reason;
    private final String detail;

    public DisconnectPeerRequest(PeerAddress peerAddress, PeerDisconnectedEvent.DisconnectedReason reason, String detail) {
        this.peerAddress = peerAddress;
        this.reason = reason;
        this.detail = detail;
    }

    public DisconnectPeerRequest(PeerAddress peerAddress, PeerDisconnectedEvent.DisconnectedReason reason) {
        this(peerAddress, reason, null);
    }

    public DisconnectPeerRequest(PeerAddress peerAddress) {
        this(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL, null);
    }

    public DisconnectPeerRequest(PeerAddress peerAddress, String detail) {
        this(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL, detail);
    }

    public PeerAddress getPeerAddress()                         { return this.peerAddress; }
    public PeerDisconnectedEvent.DisconnectedReason getReason() { return this.reason; }
    public String getDetail()                                   { return this.detail; }

    @Override
    public String toString() {
        return "DisconnectPeerRequest(peerAddress=" + this.getPeerAddress() + ", reason=" + this.getReason() + ", detail=" + this.getDetail() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        DisconnectPeerRequest other = (DisconnectPeerRequest) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.reason, other.reason)
                && Objects.equal(this.detail, other.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, reason, detail);
    }

}
