package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.net.network.events.PeerDisconnectedEvent;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Disconnect from one Peer. It also might include a Reason for that.
 */
@Value
@Builder(toBuilder = true)
public class DisconnectPeerRequest extends Event {

    private PeerAddress peerAddress;
    private PeerDisconnectedEvent.DisconnectedReason reason;
    private String detail;

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

}
