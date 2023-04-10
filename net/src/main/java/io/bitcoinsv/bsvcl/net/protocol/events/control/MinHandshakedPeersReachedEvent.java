package io.bitcoinsv.bsvcl.net.protocol.events.control;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.events.P2PEvent;

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

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        MinHandshakedPeersReachedEvent other = (MinHandshakedPeersReachedEvent) obj;
        return Objects.equal(this.numPeers, other.numPeers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), numPeers);
    }
}
