package com.nchain.jcl.net.network.events;


import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.net.InetAddress;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
