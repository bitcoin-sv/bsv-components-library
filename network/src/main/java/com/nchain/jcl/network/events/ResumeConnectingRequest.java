package com.nchain.jcl.network.events;

import com.nchain.jcl.tools.events.Event;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 15:55
 *
 * An Event that represents a Request to Resume connecting to other Peers in the Network.
 * This Event is usually triggered when the number of connections has dropped below some
 * defined threshold.
 */
@Value
public class ResumeConnectingRequest extends Event {}
