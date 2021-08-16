/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;

import io.bitcoinsv.jcl.net.network.events.P2PEvent;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when the minimun number of Peers Handshaked has been reached, as specified in the P2P
 * Configuration.
 */
public final class MinHandshakedPeersReachedEvent extends P2PEvent {
    private final int numPeers;

    public MinHandshakedPeersReachedEvent(int numPeers) {
        this.numPeers = numPeers;
    }

    public int getNumPeers() { return this.numPeers; }

    @Override
    public String toString() {
        return "MinHandshakedPeersReachedEvent(numPeers=" + this.getNumPeers() + ")";
    }
}
