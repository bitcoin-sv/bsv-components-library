package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when the Handshake with a Remote Peer has been rejected.
 */
public final class PeerHandshakeRejectedEvent extends Event {

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
