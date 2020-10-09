package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer is Connected. This is a physical connection (Socket Connection),
 * so the real communication with this Peer has not even started yet. Most probably you will be interested in the
 * PeerHandshakedEvent, which is triggered when a Peer is connected and the handshake is done, so real
 * communication can be performed.
 */
@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class PeerConnectedEvent extends Event {
    private PeerAddress peerAddress;

    @Override
    public String toString() {
        return "Event[Peer Connected]: " + peerAddress.toString();
    }

}
