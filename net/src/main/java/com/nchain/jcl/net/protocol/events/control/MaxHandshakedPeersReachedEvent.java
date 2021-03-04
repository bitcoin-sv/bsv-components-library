package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-06 14:01
 *
 * An Event triggered when the Maximun number of Pers Handshaked has been reached, as specified in the P2P
 * Configuration.
 */
public final class MaxHandshakedPeersReachedEvent extends Event {
    private final int numPeers;

    public MaxHandshakedPeersReachedEvent(int numPeers) {
        this.numPeers = numPeers;
    }

    public int getNumPeers() { return this.numPeers; }

    @Override
    public String toString() {
        return "MaxHandshakedPeersReachedEvent(numPeers=" + this.getNumPeers() + ")";
    }
}
