package com.nchain.jcl.net.network.events;

import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

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
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class PeersBlacklistedEvent extends Event {
    /**
     * Definition of all the Reasons why a Peer can be blacklisted
     */
    @AllArgsConstructor
    public enum BlacklistReason {
        CONNECTION_REJECTED (Optional.of(Duration.ofDays(1))),
        SERIALIZATION_ERROR (Optional.empty()),
        FAILED_HANDSHAKE    (Optional.empty()),
        PINGPONG_TIMEOUT    (Optional.of(Duration.ofMinutes(40))),
        CLIENT              (Optional.empty()) // Blacklisted by the Client of JCL
        ;

        @Getter private Optional<Duration> expirationTime;
    }
    private Map<InetAddress, BlacklistReason> inetAddress;

    @Override
    public String toString() {
        return "Event[Peers Blacklisted]: " + inetAddress.size() + " IPs blacklisted";
    }
}
