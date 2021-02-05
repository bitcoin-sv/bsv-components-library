package com.nchain.jcl.net.network.events;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
