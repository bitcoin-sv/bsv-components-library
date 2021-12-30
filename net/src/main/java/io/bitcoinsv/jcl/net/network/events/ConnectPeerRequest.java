package io.bitcoinsv.jcl.net.network.events;

import io.bitcoinsv.jcl.net.network.PeerAddress;

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

    public String toString() {
        return "ConnectPeerRequest(peerAddres=" + this.getPeerAddres() + ")";
    }
}
