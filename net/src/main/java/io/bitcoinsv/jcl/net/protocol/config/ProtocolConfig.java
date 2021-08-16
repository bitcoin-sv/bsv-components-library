/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.config;


import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;

import java.util.Map;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations to deserialize Configuration information of the P2P that is running.
 *
 * The "P2P" is just a set of different P2P-Handlers, each one of them taking care of
 * one specific aspect (Handshake, node-Discovery, blacklist, etc). Each one of those Handlers
 * have a HandlerConfiguration Class, so the Global P2P Configuration is the sum of all the
 * individual Handler Configuration classes.
 *
 * All these configuration classes are stored in a MAP, where the key is the Handler ID, and the Value
 * is the Configuration class itself.
 *
 * The P2P is flexible in the sense that additional handler can be included in it in real time, so
 * thats why the Handlers Configurations are sored in a Map, since we don't know how many are there, it
 * depends on the handlers that have been added to the P2P in real time
 *
 * But even though the P2P is flexible nad the number of Handlers (and therefore Handler configurations)
 * is flexible, there is a DEFAULT set of Handlers which are mandatory if we want the protocol to work.
 * These handlers are:
 * - Network Handler
 * - Message Handler
 * - Handshake Handler
 * - PingPong Handler
 * - Discovery Handler
 * - Blacklist Handler
 *
 * For convenience, this interface contains operations to deal with these configuration above specifically.
 */

public interface ProtocolConfig{
    /** Returns a Configuration ID (for logging, mostly) */
    String getId();

    /**
     * Returns the Basic P2P Configuration. This is a minimal set of variables that are most interesting
     * from the business standpoint.
     */
    ProtocolBasicConfig getBasicConfig();

    /** Returns the MAp containing the configuration for all the Handlers */
    Map<String, HandlerConfig> getHandlersConfig();

    // Convenience methods: They return the Configuration for the Default built-in Handlers
    MessageHandlerConfig getMessageConfig();
    HandshakeHandlerConfig getHandshakeConfig();
    PingPongHandlerConfig getPingPongConfig();
    DiscoveryHandlerConfig getDiscoveryConfig();
    BlacklistHandlerConfig getBlacklistConfig();
    BlockDownloaderHandlerConfig getBlockDownloaderConfig();

    /** Header of the Genesis Block of this Network */
    BlockHeaderMsg getGenesisBlock();

    default ProtocolConfigImpl.ProtocolConfigImplBuilder toBuilder() {
        return new ProtocolConfigImpl.ProtocolConfigImplBuilder();
    }
}
