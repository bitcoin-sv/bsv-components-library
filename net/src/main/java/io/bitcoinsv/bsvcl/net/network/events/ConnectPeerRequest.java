package io.bitcoinsv.bsvcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Connect to one specific Peer.
 */
public final class ConnectPeerRequest extends P2PRequest {
    private final PeerAddress peerAddres;

    public ConnectPeerRequest(PeerAddress peerAddres)   { this.peerAddres = peerAddres; }
    public PeerAddress getPeerAddres()                  { return this.peerAddres; }

    @Override
    public String toString() {
        return "ConnectPeerRequest(peerAddres=" + this.getPeerAddres() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        ConnectPeerRequest other = (ConnectPeerRequest) obj;
        return Objects.equal(this.peerAddres, other.peerAddres);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddres);
    }
}
