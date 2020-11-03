package com.nchain.jcl.net.protocol.handlers.discovery;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.config.ProtocolVersion;
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

    private ProtocolBasicConfig basicConfig;

    @Builder.Default
    private DiscoveryMethod discoveryMethod = DiscoveryMethod.DNS;
    @Builder.Default
    private Optional<Duration>  ADDRFrequency = Optional.of(Duration.ofMinutes(30));
    @Builder.Default
    private OptionalInt         ADDRPercentage = OptionalInt.of(50);;
    @Builder.Default
    private OptionalInt         maxAddresses = OptionalInt.of(1000);
    @Builder.Default
    private OptionalInt         minVersion = OptionalInt.of(ProtocolVersion.CURRENT.getBitcoinProtocolVersion());
    @Builder.Default
    private OptionalInt         relayMinAddresses = OptionalInt.of(500);
    @Builder.Default
    private Optional<Duration>  recoveryHandshakeFrequency = Optional.of(Duration.ofMinutes(15));
    @Builder.Default
    private Optional<Duration>  recoveryHandshakeThreshold = Optional.of(Duration.ofMinutes(15));
    @Builder.Default
    private boolean             checkingPeerReachability = true;
}
