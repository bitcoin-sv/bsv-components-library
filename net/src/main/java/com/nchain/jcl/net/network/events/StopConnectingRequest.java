package com.nchain.jcl.net.network.events;


import com.nchain.jcl.tools.events.Event;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Stop Connecting to more Peers in the Network.
 * This Request is usually triggered when we reach the minimum number of desired connections.
 */
@Value
public class StopConnectingRequest extends Event {
}
