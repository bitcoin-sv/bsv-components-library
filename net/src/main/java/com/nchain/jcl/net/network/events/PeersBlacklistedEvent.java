package com.nchain.jcl.net.network.events;

import com.nchain.jcl.tools.events.Event;

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
public final class PeersBlacklistedEvent extends Event {

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

    private final Map<InetAddress, BlacklistReason> inetAddress;

    public PeersBlacklistedEvent(Map<InetAddress, BlacklistReason> inetAddress) {
        this.inetAddress = inetAddress;
    }

    public Map<InetAddress, BlacklistReason> getInetAddress() {
        return this.inetAddress;
    }

    @Override
    public String toString() {
        return "Event[Peers Blacklisted]: " + inetAddress.size() + " IPs blacklisted";
    }
}
