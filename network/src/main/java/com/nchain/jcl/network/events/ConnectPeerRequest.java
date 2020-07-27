package com.nchain.jcl.network.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 16:54
 *
 * An Event that represents a Request to Connect to one specific Peer.
 */
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class ConnectPeerRequest extends Event {
    private PeerAddress peerAddres;
}
