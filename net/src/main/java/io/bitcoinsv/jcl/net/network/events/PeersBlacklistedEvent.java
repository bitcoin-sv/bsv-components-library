/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.network.events;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Event triggered when a set of Nodes is blacklisted. (Ahole IP address is blacklisted,
 * no mater the port number).
 * It also provides information about the REASON why it's been blacklisted, which also contains
 * the expirationTime (the date after which this Peer is whitelisted and can be used again).
 */
public final class PeersBlacklistedEvent extends P2PEvent {

    /**
     * Definition of all the Reasons why a Peer can be blacklisted
     */
    public enum BlacklistReason {
        CONNECTION_REJECTED (Optional.of(Duration.ofDays(1))),
        SERIALIZATION_ERROR (Optional.empty()),
        FAILED_HANDSHAKE    (Optional.empty()),
        PINGPONG_TIMEOUT    (Optional.of(Duration.ofMinutes(40))),
        CLIENT              (Optional.empty()) // Blacklisted by the Client of JCL
        ;

        private Optional<Duration> expirationTime;

        private BlacklistReason(Optional<Duration> expirationTime)  { this.expirationTime = expirationTime; }
        public Optional<Duration> getExpirationTime()               { return this.expirationTime; }
    }

    private final Map<InetAddress, BlacklistReason> inetAddresses;

    public PeersBlacklistedEvent(Map<InetAddress, BlacklistReason> inetAddress) {
        this.inetAddresses = inetAddress;
    }

    public Map<InetAddress, BlacklistReason> getInetAddresses() {
        return this.inetAddresses;
    }

    @Override
    public String toString() {
        return "Event[Peers Blacklisted]: " + inetAddresses.size() + " IPs blacklisted";
    }
}
