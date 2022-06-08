package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;

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

    @Override
    public String toString() {
        return "DisconnectPeersRequest(peersToDisconnect=" + this.getPeersToDisconnect() + ", peersToKeep=" + this.getPeersToKeep() + ", reason=" + this.getReason() + ", detail=" + this.getDetail() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        DisconnectPeersRequest other = (DisconnectPeersRequest) obj;
        return Objects.equal(this.peersToDisconnect, other.peersToDisconnect)
                && Objects.equal(this.peersToKeep, other.peersToKeep)
                && Objects.equal(this.reason, other.reason)
                && Objects.equal(this.detail, other.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peersToDisconnect, peersToKeep, reason, detail);
    }

}
