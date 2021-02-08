package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event Triggered when a connection to a Peer has been Rejected. So the connectin never took place in
 * the first place.
 */
public final class PeerRejectedEvent extends Event {

    /** Different Reasons why the conneciton has been rejected */
    public enum RejectedReason {
        INTERNAL_ERROR,
        TIMEOUT
    }
    private final PeerAddress peerAddress;
    private final RejectedReason reason;
    private final String detail; // might be null

    public PeerRejectedEvent(PeerAddress peerAddress, RejectedReason reason, String detail) {
        this.peerAddress = peerAddress;
        this.reason = reason;
        this.detail = detail;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public RejectedReason getReason()   { return this.reason; }
    public String getDetail()           { return this.detail; }

    @Override
    public String toString() {
        return "Event[PerRejected]: " + peerAddress.toString() + " : " + reason + " : " + ((detail != null)? detail : " no reason specified");
    }
}
