package com.nchain.jcl.protocol.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:56
 *
 * A Request to Enable the Ping/Pong protocol for a particular Peer
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class EnablePingPongRequest extends Event {
    private PeerAddress peerAddress;
}
