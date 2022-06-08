package io.bitcoinsv.jcl.net.protocol.events.control;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.events.P2PEvent;

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

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        MinHandshakedPeersLostEvent other = (MinHandshakedPeersLostEvent) obj;
        return Objects.equal(this.numPeers, other.numPeers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), numPeers);
    }
}
