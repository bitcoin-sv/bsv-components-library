package io.bitcoinsv.bsvcl.net.protocol.events.control;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.events.P2PEvent;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-06 14:01
 *
 * An Event triggered when the Maximun number of Pers Handshaked has been reached, as specified in the P2P
 * Configuration.
 */
public final class MaxHandshakedPeersReachedEvent extends P2PEvent {
    private final int numPeers;

    public MaxHandshakedPeersReachedEvent(int numPeers) {
        this.numPeers = numPeers;
    }

    public int getNumPeers() { return this.numPeers; }

    @Override
    public String toString() {
        return "MaxHandshakedPeersReachedEvent(numPeers=" + this.getNumPeers() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        MaxHandshakedPeersReachedEvent other = (MaxHandshakedPeersReachedEvent) obj;
        return Objects.equal(this.numPeers, other.numPeers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), numPeers);
    }
}
