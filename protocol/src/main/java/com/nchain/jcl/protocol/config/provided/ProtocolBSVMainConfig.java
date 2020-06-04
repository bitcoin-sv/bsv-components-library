package com.nchain.jcl.protocol.config.provided;

import com.nchain.jcl.protocol.config.ProtocolConfigImpl;
import com.nchain.jcl.protocol.config.ProtocolServices;
import com.nchain.jcl.protocol.config.ProtocolVersion;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

/**
 *
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-12
 *
 * A connection configuration for the Main Network.
 */
public class ProtocolBSVMainConfig extends ProtocolConfigImpl {

    // Basic: Validation & Behaviour:

    private static final String              id = "bsv-mainnet";
    private static final long                magicPackage = 0xe8f3e1e3L; // sent over the wire as e3e1f3e8L;
    private static final int                 servicesSupported = ProtocolServices.NODE_BLOOM.getProtocolServices();;
    private static final int                 port = 8333;
    private static final int                 maxMessageSizeInBytes = Integer.MAX_VALUE; // Dummy Value;
    private static final boolean             checkingMessageChecksum = true;
    private static final boolean             blockingOnListeners = false;
    private static final boolean             disconnectingOnUnknownMessage = false;
    private static final long                largeMsgsMinSize = 10_000_000; // 10MB

    // Node Discovery:
    private static final String[]            discoveryDnsSeeds = new String[] {
            "seed.bitcoinsv.io",
            "seed.cascharia.com",
            "seed.satoshivision.network"
    };
    private static final DiscoveryMethod    discoveryMethod = DiscoveryMethod.DNS;
    private static Optional<Duration>       discoveryADDRFrequency = Optional.of(Duration.ofMinutes(30));
    private static OptionalInt              discoveryADDRPercentage = OptionalInt.of(50);

    private static final OptionalInt        discoveryMaxAddresses = OptionalInt.of(1000);
    private static final OptionalInt        discoveryMinVersion = OptionalInt.of(ProtocolVersion.CURRENT.getBitcoinProtocolVersion());
    private static final OptionalInt        discoveryRelayMinAddresses = OptionalInt.of(500);
    private static final boolean            discoveryCheckingPeerReachability = true;

    // Handshake:
    private static final int                 handshakeProtocolVersion = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();
    private static final String              bitcoinSVVersion = "0.0.7";
    private static final String              handshakeUserAgent = "/bitcoinj-sv:" + bitcoinSVVersion + "/";;
    private static final OptionalInt         handshakeMaxPeers = OptionalInt.empty();
    private static final OptionalInt         handshakeMinPeers = OptionalInt.empty();
    private static final String[]            handshakeBlacklistPatterns = new String[] {
            "Bitcoin ABC:",
            "BUCash:"
    };
    private static final String[]            handshakeWhitelistPatterns = new String[] {
            "Bitcoin SV:",
            handshakeUserAgent
    };
    private static final boolean             handshakeUsingRelay = true; // TODO: CAREFUL!!


    // ping/Pong:
    private static final long                pingPongInactivityTimeout = 240000; // 3 minutes
    private static final long                pingPongResponseTimeout = 180000; // 2 minutes

    // Blacklist:
    private static final boolean            blacklistPersistent = true;

    // Block downloading:
    private static final int                blockDownloadingMaxBlocksInParallel = 10;
    private static final Duration           blockDownloadingAndSerializationTimeout = Duration.ofMinutes(20);
    private static final Duration           blockDownloadingIdleTimeout = Duration.ofSeconds(30);
    private static int                      blockDownloadingMaxAttempts = 3; // 10 attempts
    private static Duration                 blockSerializationTimeout = Duration.ofMinutes(15);
    // In the BSV-Main Network, Blocks size average are less than 10MB:
    private static int                      blockAsyncDownloadBatchMaxTxsLengthInBytes = 5_000_000; // 1MB
    private static int                      blockAsyncDownloadBatchMaxTxsNum = 5000;

    /** Constructor */
    public ProtocolBSVMainConfig() {
        super(
                id,
                magicPackage,
                servicesSupported,
                port,
                maxMessageSizeInBytes,
                checkingMessageChecksum,
                blockingOnListeners,
                disconnectingOnUnknownMessage,
                largeMsgsMinSize,
                discoveryDnsSeeds,
                discoveryMethod,
                discoveryADDRFrequency,
                discoveryADDRPercentage,
                discoveryMaxAddresses,
                discoveryMinVersion,
                discoveryRelayMinAddresses,
                discoveryCheckingPeerReachability,
                handshakeProtocolVersion,
                handshakeUserAgent,
                handshakeMaxPeers,
                handshakeMinPeers,
                handshakeBlacklistPatterns,
                handshakeWhitelistPatterns,
                handshakeUsingRelay,
                pingPongInactivityTimeout,
                pingPongResponseTimeout,
                blacklistPersistent,
                blockDownloadingMaxBlocksInParallel,
                blockDownloadingAndSerializationTimeout,
                blockDownloadingIdleTimeout,
                blockDownloadingMaxAttempts,
                blockSerializationTimeout,
                blockAsyncDownloadBatchMaxTxsLengthInBytes,
                blockAsyncDownloadBatchMaxTxsNum);
    }
}