package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event Triggered when the Network Activity Starts
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class NetStartEvent extends Event {
    // Local Address of our PRocess:
    private PeerAddress localAddress;
}
