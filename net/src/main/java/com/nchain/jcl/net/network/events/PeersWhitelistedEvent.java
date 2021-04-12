package com.nchain.jcl.net.network.events;


import com.nchain.jcl.tools.events.Event;

import java.net.InetAddress;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An event triggered when a set of IP Addresses has been whitelisted (back to business again)
 */
public final class PeersWhitelistedEvent extends P2PEvent {
    private final List<InetAddress> inetAddresses;

    public PeersWhitelistedEvent(List<InetAddress> inetAddresses) {
        this.inetAddresses = inetAddresses;
    }

    @Override
    public String toString() {
        return "Event[Peer Whitelisted]: "
                + ((inetAddresses.size() == 1)
                    ? inetAddresses.get(0).toString()
                    : inetAddresses.size() + " IPs whitelisted");
    }

    public List<InetAddress> getInetAddresses() {
        return this.inetAddresses;
    }


}
