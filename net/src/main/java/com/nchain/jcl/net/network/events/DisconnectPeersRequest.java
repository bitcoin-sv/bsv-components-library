package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Disconnect from a list of Peers. This Request allows for 2 types of
 * Disconnections:
 * - We can just disconnect from List of Peers (normal way)
 * - We can just request to disconnect from ALL the current connected peers EXCEPT the ones provided by another list.
 */
public final class DisconnectPeersRequest extends P2PRequest {
    private final List<PeerAddress> peersToDisconnect;
    private final List<PeerAddress> peersToKeep;
    private final PeerDisconnectedEvent.DisconnectedReason reason;
    private final String detail;

    public DisconnectPeersRequest(List<PeerAddress> peersToDisconnect, List<PeerAddress> peersToKeep, PeerDisconnectedEvent.DisconnectedReason reason, String detail) {
        this.peersToDisconnect = peersToDisconnect;
        this.peersToKeep = peersToKeep;
        this.reason = reason;
        this.detail = detail;
    }

    public static DisconnectPeersRequestBuilder builder() {
        return new DisconnectPeersRequestBuilder();
    }

    public List<PeerAddress> getPeersToDisconnect() {
        return this.peersToDisconnect;
    }

    public List<PeerAddress> getPeersToKeep() {
        return this.peersToKeep;
    }

    public PeerDisconnectedEvent.DisconnectedReason getReason() {
        return this.reason;
    }

    public String getDetail() {
        return this.detail;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DisconnectPeersRequest)) return false;
        final DisconnectPeersRequest other = (DisconnectPeersRequest) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$peersToDisconnect = this.getPeersToDisconnect();
        final Object other$peersToDisconnect = other.getPeersToDisconnect();
        if (this$peersToDisconnect == null ? other$peersToDisconnect != null : !this$peersToDisconnect.equals(other$peersToDisconnect))
            return false;
        final Object this$peersToKeep = this.getPeersToKeep();
        final Object other$peersToKeep = other.getPeersToKeep();
        if (this$peersToKeep == null ? other$peersToKeep != null : !this$peersToKeep.equals(other$peersToKeep))
            return false;
        final Object this$reason = this.getReason();
        final Object other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) return false;
        final Object this$detail = this.getDetail();
        final Object other$detail = other.getDetail();
        if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof DisconnectPeersRequest;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $peersToDisconnect = this.getPeersToDisconnect();
        result = result * PRIME + ($peersToDisconnect == null ? 43 : $peersToDisconnect.hashCode());
        final Object $peersToKeep = this.getPeersToKeep();
        result = result * PRIME + ($peersToKeep == null ? 43 : $peersToKeep.hashCode());
        final Object $reason = this.getReason();
        result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
        final Object $detail = this.getDetail();
        result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
        return result;
    }

    public String toString() {
        return "DisconnectPeersRequest(peersToDisconnect=" + this.getPeersToDisconnect() + ", peersToKeep=" + this.getPeersToKeep() + ", reason=" + this.getReason() + ", detail=" + this.getDetail() + ")";
    }

    public DisconnectPeersRequestBuilder toBuilder() {
        return new DisconnectPeersRequestBuilder().peersToDisconnect(this.peersToDisconnect).peersToKeep(this.peersToKeep).reason(this.reason).detail(this.detail);
    }

    public static class DisconnectPeersRequestBuilder {
        private List<PeerAddress> peersToDisconnect;
        private List<PeerAddress> peersToKeep;
        private PeerDisconnectedEvent.DisconnectedReason reason;
        private String detail;

        DisconnectPeersRequestBuilder() {
        }

        public DisconnectPeersRequest.DisconnectPeersRequestBuilder peersToDisconnect(List<PeerAddress> peersToDisconnect) {
            this.peersToDisconnect = peersToDisconnect;
            return this;
        }

        public DisconnectPeersRequest.DisconnectPeersRequestBuilder peersToKeep(List<PeerAddress> peersToKeep) {
            this.peersToKeep = peersToKeep;
            return this;
        }

        public DisconnectPeersRequest.DisconnectPeersRequestBuilder reason(PeerDisconnectedEvent.DisconnectedReason reason) {
            this.reason = reason;
            return this;
        }

        public DisconnectPeersRequest.DisconnectPeersRequestBuilder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public DisconnectPeersRequest build() {
            return new DisconnectPeersRequest(peersToDisconnect, peersToKeep, reason, detail);
        }

        public String toString() {
            return "DisconnectPeersRequest.DisconnectPeersRequestBuilder(peersToDisconnect=" + this.peersToDisconnect + ", peersToKeep=" + this.peersToKeep + ", reason=" + this.reason + ", detail=" + this.detail + ")";
        }
    }
}
