package com.nchain.jcl.net.network.events;

import com.nchain.jcl.base.tools.events.Event;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 15:55
 *
 * An Event that represents a Request to Stop Connecting to more Peers in the Network.
 * This Request is usually triggered when we reach the minimum number of desired connections.
 */
@Value
public class StopConnectingRequest extends Event {
}
