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
public class ProtocolBTCMainConfig extends ProtocolConfigImpl {

    // Basic: Validation & Behaviour:

    private static final String              id = "btc-mainnet";
    private static final long                magicPackage = 0xd9b4bef9L;  //0xf9beb4d9L;
    private static final int                 servicesSupported = ProtocolServices.NODE_BLOOM.getProtocolServices();;
    private static final int                 port = 8333;
    private static final int                 maxMessageSizeInBytes = Integer.MAX_VALUE; // Dummy Value;
    private static final boolean             checkingMessageChecksum = true;
    private static final boolean             blockingOnListeners = false;
    private static final boolean             disconnectingOnUnknownMessage = false;
    private static final long                largeMsgsMinSize = 10_000_000; // 10MB

    // Node Discovery:
    private static final String[]            discoveryDnsSeeds = new String[] {
            "seed.bitcoin.sipa.be",         // Pieter Wuille
            "dnsseed.bluematt.me",          // Matt Corallo
            "dnsseed.bitcoin.dashjr.org",   // Luke Dashjr
            "seed.bitcoinstats.com",        // Chris Decker
            "seed.bitcoin.jonasschnelli.ch",// Jonas Schnelli
            "seed.btc.petertodd.org",       // Peter Todd
            "seed.bitcoin.sprovoost.nl",    // Sjors Provoost
            "dnsseed.emzy.de",              // Stephan Oeste
    };
    private static final DiscoveryMethod    discoveryMethod = DiscoveryMethod.DNS;
    private static Optional<Duration>       discoveryADDRFrequency = Optional.of(Duration.ofMinutes(60));
    private static OptionalInt              discoveryADDRPercentage = OptionalInt.of(30);

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
            "Bitcoin SV:",
            "BUCash:"
    };
    private static final String[]            handshakeWhitelistPatterns = null;
    private static final boolean             handshakeUsingRelay = true; // TODO: CAREFUL!!


    // ping/Pong:
    private static final long                pingPongInactivityTimeout = 240000; // 3 minutes
    private static final long                pingPongResponseTimeout = 180000; // 2 minutes

    // Blacklist:
    private static final boolean            blacklistPersistent = true;

    // Block downloading:
    private static final int                blockDownloadingMaxBlocksInParallel = 10;
    private static final Duration           blockDownloadingAndSerializationTimeout = Duration.ofMinutes(5);
    private static final Duration           blockDownloadingIdleTimeout = Duration.ofSeconds(30);
    private static int                      blockDownloadingMaxAttempts = 3; // 10 attempts
    private static Duration                 blockSerializationTimeout = Duration.ofMinutes(10);

    // In the BTC-Main Network, Blocks size are below 1MB:
    private static int                      blockAsyncDownloadBatchMaxTxsLengthInBytes = 1_000_000; // 5MB
    private static int                      blockAsyncDownloadBatchMaxTxsNum = 2000;

    /** Constructor */
    public ProtocolBTCMainConfig() {
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