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
 * An Event triggered when a Peer is Connected. This is a physical connection (Socket Connection),
 * so the real communication with this Peer has not even started yet. Most probably you will be interested in the
 * PeerHandshakedEvent, which is triggered when a Peer is connected and the handshake is done, so real
 * communication can be performed.
 */
public final class PeerConnectedEvent extends P2PEvent {
    private final PeerAddress peerAddress;

    public PeerConnectedEvent(PeerAddress peerAddress)  { this.peerAddress = peerAddress; }
    public PeerAddress getPeerAddress()                 { return this.peerAddress; }
    @Override
    public String toString() {
        return "Event[Peer Connected]: " + peerAddress.toString();
    }

}
