package com.nchain.jcl.protocol.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-11
 *
 * Configuration interface for the Protocol Component. Implementations will provide specific values
 * for different Protocol configurations (like mainnet, testnet, or specific values to define custom
 * behaviour)
 */
public interface ProtocolConfig {

    // Basic Configuration:

    /** Returns this Configuration Id */
    String getId();
    /** Returns the Magic Value used in the Messages Header, to identify the Network */
    long getMagicPackage();
    /** Returns the Port used to connect to remote Peers */
    int getPort();
    /** Returns the Max size of a Message. Any incoming Message bigger than this will be considered incorrect */
    int getMaxMessageSizeInBytes();
    /** Indicates if the messages checksum is checked during Serialization */
    boolean isCheckingMessageChecksum();
    /** Returns the Number indicating the services supported */
    int getServicesSupported();
    /** Indicates if the connection to a Remote Peer is disconnected when an unknown message is received, or just ignored */
    boolean isDisconnectingOnUnknownMessage();
    /** Indicates if the Listeners at protocol Level run in the same Thread (blocking) or not (not blocking) */
    boolean isBlockingOnListeners();

    /**
     * If we receive a message with a size equals or bigger, then it must be serialized ashyncronously
     * as a "Large Message"
     */
    long getLargeMsgsMinSize();


    // Node Discovery Configuration:

    enum DiscoveryMethod {
        DNS,                // Hardcoded DNS List
        PEERS               // Hardcoded List of initial Peers
    }

    /** Returns a list of DNS Seed used for Initial Node Discovery */
    String[] getDiscoveryDnsSeeds();

    /** Returns the Discovery Method used */
    DiscoveryMethod getDiscoveryMethod();

    /**
     * Returns how often we ask for new Addresses to other Nodes. If not present, we do NOT ask for Addresses at all
     */
    Optional<Duration> getDiscoveryADDRFrequency();

    /**
     * If presents, specifies how many Peers we ask for new Addresses. Its a percentage, so if for example we are
     * connected to 200 Peers (not only connected, but also handshaked) and this value is 30, then 60 Peers are
     * randomly selected and we send GETADDR messages to them.
     */
    OptionalInt getDiscoveryADDRPercentage();

    /** Max number of Addresses we store. If we already have this much, we ignore all further incoming ADDR. */
    OptionalInt getDiscoveryMaxAddresses();

    /** If the ADDR Message is coming from a Peer which version is less than this one, then the Message is ignored. */
    OptionalInt getDiscoveryMinVersion();

    /** Minimum number of Address we isHandshakeUsingRelay to to other Peers. If we have less than this, we do not rely them. */
    OptionalInt getDiscoveryRelayMinAddresses();

    /** If TRUE, each Address discovered in the Network will be checks for reachability before trying to stablish */
    boolean isDiscoveryCheckingPeerReachability();

    // HANDSHAKE Configuration parameters:


    /** Returns the Protocol Version used */
    int getHandshakeProtocolVersion();
    /** The USER_AGENT Field used in the VERSION Messages, during the Handshake */
    String getHandshakeUserAgent();
    /**
     * Minimun number of Peers to perform a Handshake with. If the number of current handshakes goes below this
     * number, the Network Handler will start accepting new connections again.
     */
    OptionalInt getHandshakeMinPeers();
    /**
     * Maximum number of Peers to perform a Handshake with . If the Handshakes performed
     * exceed this number, The Network Handler will stop accepting new connections and will disconnect those ones
     * that exceed this number.
     */
    OptionalInt getHandshakeMaxPeers();


    /**
     * If a VERSION Msg coming from a Remote Peer during the Handshake contains any of these Strings, the
     * Handshake is Failed.
     */
    String[] getHandshakeBlacklistPatterns();

    /**
     * If a VERSION Msg coming from a Remote Peer during the Handshake does NOT contains at least one of these String, the
     * Handshake is Failed
     */
    String[] getHandshakeWhitelistPatterns();


    /** Indicates whether the remote peer should announce relayed Transactions or not, see BIP 0037 */
    boolean isHandshakeUsingRelay();

    // PING/PONG Configuration parameters:

    /**
     * If a Peer is inactive during a period of time longer than this value, then we start the PING-PONG
     * Protocol with it, to ensure the connection is still alive.
     * The time is measured in milliseconds
     */
    long getPingPongInactivityTimeout();

    /**
     * If, after sending a PING Message to a Peer, it takes it longer time that this value, then we assume
     * that the connection is not alive.
     * The time is measured in milliseconds
     */
    long getPingPongResponseTimeout();

    // BLACKLIST Configuration parameters:

    /** If TRue, the List of blacklisted Hosts is stored so it can be loaded next time we launch the server */
    boolean isBlacklistPersistent();

    // BLOCK DOWNLOAD configuration:

    /** Maximun number of Blocks that can be downloaded at the same time */
    int getBlockDownloadingMaxBlocksInParallel();

    /** If downloading a block from a remote Peer takes more time than this, the peer is disconnected */
    Duration getBlockDownloadingAndSerializationTimeout();

    /** If a Peer is downloading a Block but does NOT send any data from longer than this, it gets disconnected */
    Duration getBlockDownloadingIdleTimeout();

    /** If after this number of attempts the Bocks cannot be downloaded, we give up */
    int getBlockDownloadingMaxAttempts();

    /** If Serializing/Deserializing a Block takes longer than this, it might get rejected */
    Duration getBlockSerializationTimeout();

    /**
     * When the size of a Block is bigger than "getLargeMsgsMinSize", then it will be downloaded asyncbhronously: the
     * Block Header will be downloaded first, and then the Txs. The Txs will be returned back to the client via
     * callbacks, sending a list of Txs on each call. The number of Txs on each call depends on 2 thresholds:
     *  - A constant defined im the Block Deserializer (default to 10000)
     *  - the size (in bytes) those Tx take
     */

    /**
     * If the size (in Bytes)of the TXs take more than the value returned by this method, then they will be sent to the client
     *  even if we haven't reached the threshold specified.
     */
    int getBlockAsyncDownloadBatchMaxTxsLengthInBytes();

    /**
     * If the number of TXs that are being deserialized asynchronously is bigger than this number, then they will be
     * returned to the client via callback, even if their length (in bytes) is below the threshold.
     */
    int getBlockAsyncDownloadBatchMaxTxsNum();
}
