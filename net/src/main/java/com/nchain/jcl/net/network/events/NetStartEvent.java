package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 16:06
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
