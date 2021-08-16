/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.network.events;


import io.bitcoinsv.jcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to reset the connection to a remote Peer, so this connection will NOT be
 * able to deserialize BigMessages from that peer from this moment (all the BigMesssages coming from
 * that Peer will be discarded and the connection dropped)
 */
public final class DisablePeerBigMessagesRequest extends P2PRequest {
    private PeerAddress peerAddress;
    /** Constructor */
    public DisablePeerBigMessagesRequest(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    @Override
    public String toString() {
        return "DisablePeerForBigMessages( peerAddress=" + this.peerAddress + ")";
    }
}
