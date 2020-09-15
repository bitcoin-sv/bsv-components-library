package com.nchain.jcl.net.network.events;

import com.nchain.jcl.base.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.net.InetAddress;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-23 10:24
 *
 * An event triggered when a set of IP Addresses has been whitelisted (back to business again)
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeersWhitelistedEvent extends Event {
    private List<InetAddress> inetAddresses;

    @Override
    public String toString() {
        return "Event[Peer Whitelisted]: "
                + ((inetAddresses.size() == 1)
                    ? inetAddresses.get(0).toString()
                    : inetAddresses.size() + " IPs whitelisted");
    }
}