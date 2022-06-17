package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;

import java.net.InetAddress;
import java.util.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Event triggered when a set of Nodes is whitelisted. (Whole IP address is whitelisted,
 * no matter the port number).
 */
public final class PeersWhitelistedEvent extends P2PEvent {

    private final Set<InetAddress> inetAddresses;

    public PeersWhitelistedEvent(Set<InetAddress> inetAddress) {
        this.inetAddresses = inetAddress;
    }

    public PeersWhitelistedEvent(InetAddress inetAddress) {
        inetAddresses = new HashSet<>(){{ add(inetAddress);}};
    }

    public Set<InetAddress> getInetAddresses() {
        return this.inetAddresses;
    }

    @Override
    public String toString() {
        return "Event[Peers Whitelisted]: " + inetAddresses.size() + " IPs whitelisted";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeersWhitelistedEvent other = (PeersWhitelistedEvent) obj;
        return Objects.equal(this.inetAddresses, other.inetAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), inetAddresses);
    }
}
