package io.bitcoinsv.jcl.net.network.events;


import com.google.common.base.Objects;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An event triggered when a set of IP Addresses has been removed from the Whitelist, so they are again "regular" peers
 */
public final class PeersRemovedFromWhitelistEvent extends P2PEvent {
    private final List<InetAddress> inetAddresses;

    public PeersRemovedFromWhitelistEvent(List<InetAddress> inetAddresses) {
        this.inetAddresses = inetAddresses;
    }
    public PeersRemovedFromWhitelistEvent(InetAddress inetAddress) {
        this.inetAddresses = new ArrayList<>() {{add(inetAddress);}};
    }

    @Override
    public String toString() {
        return "Event[Peer Removed from Whitelist]: "
                + ((inetAddresses.size() == 1)
                    ? inetAddresses.get(0).toString()
                    : inetAddresses.size() + " IPs removed");
    }

    public List<InetAddress> getInetAddresses() {
        return this.inetAddresses;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeersRemovedFromWhitelistEvent other = (PeersRemovedFromWhitelistEvent) obj;
        return Objects.equal(this.inetAddresses, other.inetAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), inetAddresses);
    }

}
