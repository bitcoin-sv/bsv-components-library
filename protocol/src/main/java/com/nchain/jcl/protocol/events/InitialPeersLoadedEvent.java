package com.nchain.jcl.protocol.events;

import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-13 14:17
 *
 * An Event triggered when the Discovery Handler loads the initial Set of Peers that is used to start the
 * connection process to the P2P Network.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class InitialPeersLoadedEvent extends Event {
    private int numPeersLoaded;
    private DiscoveryHandlerConfig.DiscoveryMethod discoveryMethod;
}
