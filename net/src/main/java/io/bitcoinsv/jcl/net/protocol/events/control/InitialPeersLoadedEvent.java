/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.events.P2PEvent;
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-13 14:17
 *
 * An Event triggered when the Discovery Handler loads the initial Set of Peers that is used to start the
 * connection process to the P2P Network.
 */
public final class InitialPeersLoadedEvent extends P2PEvent {
    private final int numPeersLoaded;
    private final DiscoveryHandlerConfig.DiscoveryMethod discoveryMethod;

    public InitialPeersLoadedEvent(int numPeersLoaded, DiscoveryHandlerConfig.DiscoveryMethod discoveryMethod) {
        this.numPeersLoaded = numPeersLoaded;
        this.discoveryMethod = discoveryMethod;
    }

    public int getNumPeersLoaded()                                      { return this.numPeersLoaded; }
    public DiscoveryHandlerConfig.DiscoveryMethod getDiscoveryMethod()  { return this.discoveryMethod; }

    @Override
    public String toString() {
        return "InitialPeersLoadedEvent(numPeersLoaded=" + this.getNumPeersLoaded() + ", discoveryMethod=" + this.getDiscoveryMethod() + ")";
    }
}
