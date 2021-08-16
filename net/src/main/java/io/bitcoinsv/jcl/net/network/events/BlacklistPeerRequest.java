/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.network.events;


import io.bitcoinsv.jcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:57
 *
 * A Request to Blacklist a Peer
 */
public final class BlacklistPeerRequest extends P2PRequest {
    // NOTE: We do NOT Specify a REASON, since this Request will be triggered by the Client, so its up to the Client
    // to know and keep track of that.
    private final PeerAddress peerAddress;

    public BlacklistPeerRequest(PeerAddress peerAddress)    { this.peerAddress = peerAddress; }
    public PeerAddress getPeerAddress()                     { return this.peerAddress; }

    public String toString() {
        return "BlacklistPeerRequest(peerAddress=" + this.getPeerAddress() + ")";
    }

}
