package com.nchain.jcl.network.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 16:16
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
