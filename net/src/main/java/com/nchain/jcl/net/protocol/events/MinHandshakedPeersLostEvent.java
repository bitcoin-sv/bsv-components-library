package com.nchain.jcl.net.protocol.events;

import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-06 14:01
 *
 * An Event triggered when the Number of Peers Handshakes has dropped below the threshold specified in the
 * P2P Configuration
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class MinHandshakedPeersLostEvent extends Event {
    private int numPeers;
}
