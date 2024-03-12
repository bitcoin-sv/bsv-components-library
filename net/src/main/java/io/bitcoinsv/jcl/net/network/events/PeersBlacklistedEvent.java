package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;

import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2024 nChain Ltd
 *
 * Event triggered when a set of Nodes is blacklisted. (Whole IP address is blacklisted,
 * no matter the port number).
 * It also provides information about the REASON why it's been blacklisted, which also contains
 * the expirationTime (the date after which this Peer is whitelisted and can be used again).
 */
public final class PeersBlacklistedEvent extends P2PEvent {

    /**
     * Definition of all the Reasons why a Peer can be blacklisted. For each Reason, the "duration" specified the
     * time this peer will remain Blacklisted (empty = forever)
     */
    public enum BlacklistReason {
        CONNECTION_REJECTED,
        SERIALIZATION_ERROR,
        FAILED_HANDSHAKE,
        PINGPONG_TIMEOUT,
        CLIENT // Blacklisted by the Client of JCL
    }

    private final Map<InetAddress, BlacklistReason> inetAddresses;

    public PeersBlacklistedEvent(Map<InetAddress, BlacklistReason> inetAddress) {
        this.inetAddresses = inetAddress;
    }

    public PeersBlacklistedEvent(InetAddress inetAddress, BlacklistReason reason) {
        this.inetAddresses = new HashMap<>() {{ put(inetAddress, reason); }};
    }

    public Map<InetAddress, BlacklistReason> getInetAddresses() {
        return this.inetAddresses;
    }

    @Override
    public String toString() {
        return "Event[Peers Blacklisted]: " + inetAddresses.size() + " IPs blacklisted";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeersBlacklistedEvent other = (PeersBlacklistedEvent) obj;
        return Objects.equal(this.inetAddresses, other.inetAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), inetAddresses);
    }
}
