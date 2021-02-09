package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when the minimun number of Peers Handshaked has been reached, as specified in the P2P
 * Configuration.
 */
public final class MinHandshakedPeersReachedEvent extends Event {
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
