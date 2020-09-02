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
