package io.bitcoinsv.bsvcl.net.network.events;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event Triggered when the Network Activity Starts
 */
public final class NetStartEvent extends P2PEvent {
    // Local Address of our Process:
    private final PeerAddress localAddress;

    public NetStartEvent(PeerAddress localAddress)  { this.localAddress = localAddress; }
    public PeerAddress getLocalAddress()            { return this.localAddress; }

    @Override
    public String toString() {
        return "NetStartEvent(localAddress=" + this.getLocalAddress() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        NetStartEvent other = (NetStartEvent) obj;
        return Objects.equal(this.localAddress, other.localAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), localAddress);
    }
}
