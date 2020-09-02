package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.net.network.PeerAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:57
 *
 * A Request to Disable the Ping/Pong protocol for a Particular Peer
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class DisablePingPongRequest extends Event {
    private PeerAddress peerAddress;
}
