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

    public DisconnectPeerRequest(PeerAddress peerAddress) {
        this(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL, null);
    }

    public DisconnectPeerRequest(PeerAddress peerAddress, String detail) {
        this(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL, detail);
    }

    public PeerAddress getPeerAddress()                         { return this.peerAddress; }
    public PeerDisconnectedEvent.DisconnectedReason getReason() { return this.reason; }
    public String getDetail()                                   { return this.detail; }

    public String toString() {
        return "DisconnectPeerRequest(peerAddress=" + this.getPeerAddress() + ", reason=" + this.getReason() + ", detail=" + this.getDetail() + ")";
    }

}
