package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Connect to a List of Peers.
 */
public final class ConnectPeersRequest extends P2PRequest {
    private final List<PeerAddress> peerAddressList;

    public ConnectPeersRequest(List<PeerAddress> peerAddressList)   { this.peerAddressList = peerAddressList; }
    public List<PeerAddress> getPeerAddressList()                   { return this.peerAddressList; }

    @Override
    public String toString() {
        return "ConnectPeersRequest(" + this.getPeerAddressList().size() + " peers)";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        ConnectPeersRequest other = (ConnectPeersRequest) obj;
        return Objects.equal(this.peerAddressList, other.peerAddressList);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddressList);
    }
}
