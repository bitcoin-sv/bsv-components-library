package io.bitcoinsv.bsvcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event Triggered when a connection to a Peer has been Rejected. So the connectin never took place in
 * the first place.
 */
public final class PeerRejectedEvent extends P2PEvent {

    /** Different Reasons why the conneciton has been rejected */
    public enum RejectedReason {
        INTERNAL_ERROR,
        TIMEOUT
    }
    private final PeerAddress peerAddress;
    private final RejectedReason reason;
    private final String detail; // might be null

    public PeerRejectedEvent(PeerAddress peerAddress, RejectedReason reason, String detail) {
        this.peerAddress = peerAddress;
        this.reason = reason;
        this.detail = detail;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public RejectedReason getReason()   { return this.reason; }
    public String getDetail()           { return this.detail; }

    @Override
    public String toString() {
        return "Event[PerRejected]: " + peerAddress.toString() + " : " + reason + " : " + ((detail != null)? detail : " no reason specified");
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeerRejectedEvent other = (PeerRejectedEvent) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.reason, other.reason)
                && Objects.equal(this.detail, other.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, reason, detail);
    }
}
