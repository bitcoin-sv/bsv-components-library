package com.nchain.jcl.protocol.handlers.discovery;

import com.nchain.jcl.tools.handlers.HandlerState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-13 11:31
 *
 * This event stores the state of the Discovery Handler at a point in time.
 * The Discovery Handler takes care of feeding the Service with enough addresses of Remote Peers, so
 * we have a "pool" of addresses we can use when we need to connect to more Peers.
 */
@Value
@Builder(toBuilder = true)
public class DiscoveryHandlerState extends HandlerState {
    // Size (number of Peers) in the Pool
    private long poolSize;
    // We keep track of how many Peers have been handshaked, removed from the Pool, rejected...
    private long numNodesHandshaked;
    private long numNodesAdded;
    private long numNodesRemoved;
    private long numNodesRejected;
    // We also keep track of the number of GET_ADDR and DDR messages sent and received...
    private long numGetAddrMsgsSent;
    private long numAddrMsgsReceived;

    // The information about how much addresses go into the ADDR messages is useful, to ehck if they are being
    // under or over used. Wo we use a Map to store that info:
    //  Key: Number of elements in the ADDR Msg
    //  Value: Number of ADDR messages received containing that number of addresses inside
    // example: [5,20] -> we've received 20 ADDR messages containing 5 Addresses each.

    @Builder.Default
    private Map<Integer, Integer> addrMsgsSize = new HashMap<>();

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();

        result.append("Discovery State: ");
        result.append(" Pool size: " + poolSize);
        result.append(" [ ");
        result.append(numNodesHandshaked + " handshaked, ");
        result.append(numNodesAdded + " added, ");
        result.append(numNodesRemoved + " removed, ");
        result.append(numNodesRejected + " rejected");
        result.append(" ] : ");
        result.append(numGetAddrMsgsSent + " GET_ADDR sent, ");
        result.append(numAddrMsgsReceived + " ADDR received");

        return result.toString();
    }

}
