package com.nchain.jcl.protocol.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-08-20 11:42
 *
 * Implementation of a Protocol configuration
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ProtocolConfigImpl implements ProtocolConfig {

    // Basic: Validation & Behaviour:

    private String              id;
    private long                magicPackage;
    private int                 servicesSupported;
    private int                 port;
    private int                 maxMessageSizeInBytes;
    private boolean             checkingMessageChecksum;
    private boolean             blockingOnListeners;
    private boolean             disconnectingOnUnknownMessage;
    private long                largeMsgsMinSize;

    // Node Discovery:
    private String[]            discoveryDnsSeeds;
    private DiscoveryMethod     discoveryMethod;
    private Optional<Duration>  discoveryADDRFrequency;
    private OptionalInt         discoveryADDRPercentage;

    private OptionalInt         discoveryMaxAddresses;
    private OptionalInt         discoveryMinVersion;
    private OptionalInt         discoveryRelayMinAddresses;
    private boolean             discoveryCheckingPeerReachability;

    // Handshake:
    private int                 handshakeProtocolVersion;
    private String              handshakeUserAgent;
    private OptionalInt         handshakeMaxPeers;
    private OptionalInt         handshakeMinPeers;
    private String[]            handshakeBlacklistPatterns;
    private String[]            handshakeWhitelistPatterns;
    private boolean             handshakeUsingRelay;


    // ping/Pong:
    private long                pingPongInactivityTimeout;
    private long                pingPongResponseTimeout;

    // Blacklist:
    private boolean             blacklistPersistent;

    // Block Downloading:
    private int                 blockDownloadingMaxBlocksInParallel;
    private Duration            blockDownloadingAndSerializationTimeout;
    private Duration            blockDownloadingIdleTimeout;
    private int                 blockDownloadingMaxAttempts;
    private Duration            blockSerializationTimeout;
    private int                 blockAsyncDownloadBatchMaxTxsLengthInBytes;
    private int                 blockAsyncDownloadBatchMaxTxsNum;
}
