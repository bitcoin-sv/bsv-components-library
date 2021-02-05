package com.nchain.jcl.net.protocol.handlers.discovery;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import io.bitcoinj.params.NetworkParameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the Configueation variables needed by the Discovery Handler.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryHandlerConfig extends HandlerConfig {

    // Node Discovery Configuration:
    public enum DiscoveryMethod {
        DNS,                // Hardcoded DNS List
        PEERS               // Hardcoded List of initial Peers
    }

    // Default Values:
    public static final DiscoveryMethod DEFAULT_DISCOVERY_METHOD = DiscoveryMethod.DNS;
    public static final Optional<Duration> DEFAULT_ADDR_FREQ = Optional.of(Duration.ofMinutes(30));
    public static final OptionalInt DEFAULT_ADDR_PERCENTAGE  = OptionalInt.of(50); // 50%
    public static final OptionalInt DEFAULT_MAX_ADDRESSES = OptionalInt.of(1000);
    public static final OptionalInt DEFAULT_MIN_VERSION = OptionalInt.of(NetworkParameters.ProtocolVersion.CURRENT.getBitcoinProtocolVersion());
    public static final OptionalInt DEFAULT_RELAY_MIN_ADDRESSES = OptionalInt.of(500);
    public static final Optional<Duration> DEFAULT_HANDSHAKE_RECOVERY_FREQ = Optional.of(Duration.ofMinutes(15));
    public static final Optional<Duration> DEFAULT_HANDSHAKE_RECOVERY_THRESHOLD = Optional.of(Duration.ofMinutes(15));

    /** Basic Configuration */
    private ProtocolBasicConfig basicConfig;
    /** List of DNs */
    private String[] dns;
    /** It determines how the initial set of Peers is loaded */
    @Builder.Default
    private DiscoveryMethod discoveryMethod = DEFAULT_DISCOVERY_METHOD;
    /** Frequency to send out ADDR Messages asking for Addresses */
    @Builder.Default
    private Optional<Duration> ADDRFrequency = DEFAULT_ADDR_FREQ;
    /** (documentation pending) */
    @Builder.Default
    private OptionalInt ADDRPercentage = DEFAULT_ADDR_PERCENTAGE;
    /** MAx number of Address to sen out int a ADDR Message */
    @Builder.Default
    private OptionalInt maxAddresses = DEFAULT_MAX_ADDRESSES;
    /** (documentation pending) */
    @Builder.Default
    private OptionalInt minVersion = DEFAULT_MIN_VERSION;
    /** (documentation pending) */
    @Builder.Default
    private OptionalInt relayMinAddresses = DEFAULT_RELAY_MIN_ADDRESSES;
    /** frequency of the job that reconnects to those peers that used to be handshaked but are now disconnected */
    @Builder.Default
    private Optional<Duration> recoveryHandshakeFrequency = DEFAULT_HANDSHAKE_RECOVERY_FREQ;
    @Builder.Default
    /**
     * If a Peer that used to be handshaked is now disconnected and its been disconnected for a time longer than
     * this value, then that Peer will be eligible for "renewing connection"-
     */
    private Optional<Duration> recoveryHandshakeThreshold = DEFAULT_HANDSHAKE_RECOVERY_THRESHOLD;
    @Builder.Default
    private boolean checkingPeerReachability = true;
}
