package com.nchain.jcl.net.protocol.handlers.discovery;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import io.bitcoinj.params.NetworkParameters;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the Configueation variables needed by the Discovery Handler.
 */
public class DiscoveryHandlerConfig extends HandlerConfig {

    // Default Values:
    public static final DiscoveryMethod DEFAULT_DISCOVERY_METHOD = DiscoveryMethod.DNS;
    public static final Optional<Duration> DEFAULT_ADDR_FREQ = Optional.of(Duration.ofMinutes(30));
    public static final OptionalInt DEFAULT_ADDR_PERCENTAGE  = OptionalInt.of(50); // 50%
    public static final OptionalInt DEFAULT_MAX_ADDRESSES = OptionalInt.of(1000);
    public static final OptionalInt DEFAULT_MIN_VERSION = OptionalInt.of(NetworkParameters.ProtocolVersion.CURRENT.getBitcoinProtocolVersion());
    public static final OptionalInt DEFAULT_RELAY_MIN_ADDRESSES = OptionalInt.of(500);
    public static final Optional<Duration> DEFAULT_HANDSHAKE_RECOVERY_FREQ = Optional.of(Duration.ofMinutes(15));
    public static final Optional<Duration> DEFAULT_HANDSHAKE_RECOVERY_THRESHOLD = Optional.of(Duration.ofMinutes(15));

    // Node Discovery Configuration:
    public enum DiscoveryMethod {
        DNS,                // Hardcoded DNS List
        PEERS               // Hardcoded List of initial Peers
    }

    /** Basic Configuration */
    private ProtocolBasicConfig basicConfig;
    /** List of DNs */
    private String[] dns;
    /** It determines how the initial set of Peers is loaded */
    private DiscoveryMethod discoveryMethod = DEFAULT_DISCOVERY_METHOD;
    /** Frequency to send out ADDR Messages asking for Addresses */
    private Optional<Duration> ADDRFrequency = DEFAULT_ADDR_FREQ;
    /** (documentation pending) */
    private OptionalInt ADDRPercentage = DEFAULT_ADDR_PERCENTAGE;
    /** MAx number of Address to sen out int a ADDR Message */
    private OptionalInt maxAddresses = DEFAULT_MAX_ADDRESSES;
    /** (documentation pending) */
    private OptionalInt minVersion = DEFAULT_MIN_VERSION;
    /** (documentation pending) */
    private OptionalInt relayMinAddresses = DEFAULT_RELAY_MIN_ADDRESSES;
    /** frequency of the job that reconnects to those peers that used to be handshaked but are now disconnected */
    private Optional<Duration> recoveryHandshakeFrequency = DEFAULT_HANDSHAKE_RECOVERY_FREQ;
    /**
     * If a Peer that used to be handshaked is now disconnected and its been disconnected for a time longer than
     * this value, then that Peer will be eligible for "renewing connection"-
     */
    private Optional<Duration> recoveryHandshakeThreshold = DEFAULT_HANDSHAKE_RECOVERY_THRESHOLD;
    private boolean checkingPeerReachability = true;


    public DiscoveryHandlerConfig(ProtocolBasicConfig basicConfig, String[] dns, DiscoveryMethod discoveryMethod, Optional<Duration> ADDRFrequency, OptionalInt ADDRPercentage, OptionalInt maxAddresses, OptionalInt minVersion, OptionalInt relayMinAddresses, Optional<Duration> recoveryHandshakeFrequency, Optional<Duration> recoveryHandshakeThreshold, boolean checkingPeerReachability) {
        this.basicConfig = basicConfig;
        this.dns = dns;
        if (discoveryMethod != null)            this.discoveryMethod = discoveryMethod;
        if (ADDRFrequency != null)              this.ADDRFrequency = ADDRFrequency;
        if (ADDRPercentage != null)             this.ADDRPercentage = ADDRPercentage;
        if (maxAddresses != null)               this.maxAddresses = maxAddresses;
        if (minVersion != null)                 this.minVersion = minVersion;
        if (relayMinAddresses != null)          this.relayMinAddresses = relayMinAddresses;
        if (recoveryHandshakeFrequency != null) this.recoveryHandshakeFrequency = recoveryHandshakeFrequency;
        if (recoveryHandshakeThreshold != null) this.recoveryHandshakeThreshold = recoveryHandshakeThreshold;
        this.checkingPeerReachability = checkingPeerReachability;
    }

    public DiscoveryHandlerConfig() {}

    public ProtocolBasicConfig getBasicConfig()                 { return this.basicConfig; }
    public String[] getDns()                                    { return this.dns; }
    public DiscoveryMethod getDiscoveryMethod()                 { return this.discoveryMethod; }
    public Optional<Duration> getADDRFrequency()                { return this.ADDRFrequency; }
    public OptionalInt getADDRPercentage()                      { return this.ADDRPercentage; }
    public OptionalInt getMaxAddresses()                        { return this.maxAddresses; }
    public OptionalInt getMinVersion()                          { return this.minVersion; }
    public OptionalInt getRelayMinAddresses()                   { return this.relayMinAddresses; }
    public Optional<Duration> getRecoveryHandshakeFrequency()   { return this.recoveryHandshakeFrequency; }
    public Optional<Duration> getRecoveryHandshakeThreshold()   { return this.recoveryHandshakeThreshold; }
    public boolean isCheckingPeerReachability()                 { return this.checkingPeerReachability; }

    public DiscoveryHandlerConfigBuilder toBuilder() {
        return new DiscoveryHandlerConfigBuilder().basicConfig(this.basicConfig).dns(this.dns).discoveryMethod(this.discoveryMethod).ADDRFrequency(this.ADDRFrequency).ADDRPercentage(this.ADDRPercentage).maxAddresses(this.maxAddresses).minVersion(this.minVersion).relayMinAddresses(this.relayMinAddresses).recoveryHandshakeFrequency(this.recoveryHandshakeFrequency).recoveryHandshakeThreshold(this.recoveryHandshakeThreshold).checkingPeerReachability(this.checkingPeerReachability);
    }

    public static DiscoveryHandlerConfigBuilder builder() {
        return new DiscoveryHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class DiscoveryHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;
        private String[] dns;
        private DiscoveryMethod discoveryMethod;
        private Optional<Duration> ADDRFrequency;
        private OptionalInt ADDRPercentage;
        private OptionalInt maxAddresses;
        private OptionalInt minVersion;
        private OptionalInt relayMinAddresses;
        private Optional<Duration> recoveryHandshakeFrequency;
        private Optional<Duration> recoveryHandshakeThreshold;
        private boolean checkingPeerReachability;

        DiscoveryHandlerConfigBuilder() { }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder dns(String[] dns) {
            this.dns = dns;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder discoveryMethod(DiscoveryMethod discoveryMethod) {
            this.discoveryMethod = discoveryMethod;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder ADDRFrequency(Optional<Duration> ADDRFrequency) {
            this.ADDRFrequency = ADDRFrequency;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder ADDRPercentage(OptionalInt ADDRPercentage) {
            this.ADDRPercentage = ADDRPercentage;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder maxAddresses(OptionalInt maxAddresses) {
            this.maxAddresses = maxAddresses;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder minVersion(OptionalInt minVersion) {
            this.minVersion = minVersion;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder relayMinAddresses(OptionalInt relayMinAddresses) {
            this.relayMinAddresses = relayMinAddresses;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder recoveryHandshakeFrequency(Optional<Duration> recoveryHandshakeFrequency) {
            this.recoveryHandshakeFrequency = recoveryHandshakeFrequency;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder recoveryHandshakeThreshold(Optional<Duration> recoveryHandshakeThreshold) {
            this.recoveryHandshakeThreshold = recoveryHandshakeThreshold;
            return this;
        }

        public DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder checkingPeerReachability(boolean checkingPeerReachability) {
            this.checkingPeerReachability = checkingPeerReachability;
            return this;
        }

        public DiscoveryHandlerConfig build() {
            return new DiscoveryHandlerConfig(basicConfig, dns, discoveryMethod, ADDRFrequency, ADDRPercentage, maxAddresses, minVersion, relayMinAddresses, recoveryHandshakeFrequency, recoveryHandshakeThreshold, checkingPeerReachability);
        }
    }
}
