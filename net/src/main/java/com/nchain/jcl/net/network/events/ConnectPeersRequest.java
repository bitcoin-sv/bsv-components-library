package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 16:56
 *
 * An Event that represents a Request to Connect to a List of Peers.
 */
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class ConnectPeersRequest extends Event {
    private List<PeerAddress> peerAddressList;
}
