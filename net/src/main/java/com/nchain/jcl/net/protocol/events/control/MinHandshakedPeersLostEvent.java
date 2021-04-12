package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.net.network.events.P2PEvent;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when the Number of Peers Handshakes has dropped below the threshold specified in the
 * P2P Configuration
 */
public final class MinHandshakedPeersLostEvent extends P2PEvent {
    private final int numPeers;

    public MinHandshakedPeersLostEvent(int numPeers) {
        this.numPeers = numPeers;
    }

    public int getNumPeers() { return this.numPeers; }

    @Override
    public String toString() {
        return "MinHandshakedPeersLostEvent(numPeers=" + this.getNumPeers() + ")";
    }
}
