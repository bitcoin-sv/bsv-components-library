package com.nchain.jcl.net.network.events;

import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.net.network.PeerAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is disconnected.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeerDisconnectedEvent extends Event {
    /** Definition of Reason why a Peer has been disconnected */
    public enum DisconnectedReason {
        UNDEFINED,
        DISCONNECTED_BY_LOCAL,
        DISCONNECTED_BY_REMOTE
    }
    private PeerAddress peerAddress;
    private DisconnectedReason reason;

    @Override
    public String toString() {
        return "Event[Peer Disconnected]: " + peerAddress.toString() + ": " + reason;
    }
}
