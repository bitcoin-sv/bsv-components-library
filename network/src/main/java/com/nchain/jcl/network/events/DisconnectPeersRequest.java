package com.nchain.jcl.network.events;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-13 10:16
 *
 * An Event that represents a Request to Disconnect from a list of Peers. This Request allows for 2 types of
 * Disconnections:
 * - We can just disconnect from List of Peers (normal way)
 * - We can just request to disconnect from ALL the current connected peers EXCEPT the ones provided by another list.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class DisconnectPeersRequest extends Event {
    List<PeerAddress> peersToDisconnect;
    List<PeerAddress> peersToKeep;
    private PeerDisconnectedEvent.DisconnectedReason reason;
    private String detail;
}
