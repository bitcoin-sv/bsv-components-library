package io.bitcoinsv.jcl.net.network.events;

import io.bitcoinsv.jcl.net.network.PeerAddress;

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

    public String toString() {
        return "NetStartEvent(localAddress=" + this.getLocalAddress() + ")";
    }
}
