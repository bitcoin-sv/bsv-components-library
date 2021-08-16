/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PEvent;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when the Handshake with a Remote Peer has been rejected.
 */
public final class PeerHandshakeRejectedEvent extends P2PEvent {

    /**
     * Definition of the possible reasons why a Handshake might be rejected
     */
    public enum HandshakedRejectedReason {
        PROTOCOL_MSG_DUPLICATE,
        PROTOCOL_MSG_TIMEOUT,
        WRONG_VERSION,
        WRONG_START_HEIGHT,
        WRONG_USER_AGENT
    }

    private final PeerAddress peerAddress;
    private final VersionMsg versionMsg;
    private final HandshakedRejectedReason reason;
    private final String detail;

    public PeerHandshakeRejectedEvent(PeerAddress peerAddress, VersionMsg versionMsg, HandshakedRejectedReason reason, String detail) {
        this.peerAddress = peerAddress;
        this.versionMsg = versionMsg;
        this.reason = reason;
        this.detail = detail;
    }

    public PeerAddress getPeerAddress()         { return this.peerAddress; }
    public VersionMsg getVersionMsg()           { return this.versionMsg; }
    public HandshakedRejectedReason getReason() { return this.reason; }
    public String getDetail()                   { return this.detail; }

    @Override
    public String toString() {
        return "PeerHandshakeRejectedEvent(peerAddress=" + this.getPeerAddress() + ", versionMsg=" + this.getVersionMsg() + ", reason=" + this.getReason() + ", detail=" + this.getDetail() + ")";
    }
}
