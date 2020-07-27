package com.nchain.jcl.protocol.config;

import com.nchain.jcl.network.config.NetworkConfig;
import com.nchain.jcl.network.config.NetworkConfigDefault;
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandler;
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandlerConfig;
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandler;
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.protocol.handlers.handshake.HandshakeHandler;
import com.nchain.jcl.protocol.handlers.handshake.HandshakeHandlerConfig;
import com.nchain.jcl.protocol.handlers.message.MessageHandler;
import com.nchain.jcl.protocol.handlers.message.MessageHandlerConfig;
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandler;
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandlerConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 16:30
 *
 * A Default ProtocolConfig Implementation that includes the configuration of all the built-in default
 * Handlers:
 *  - Network Handler
 *  - Message Handler
 *  - Handshake Handler
 *  - PingPong Handler
 *  - Discovery Handler
 *  - Blacklist Handler
 */

public abstract class DefaultHandlersConfig implements ProtocolConfig {

    // Network Configuration: default
    @Getter
    protected NetworkConfig networkConfig = new NetworkConfigDefault();


    // We store some references to each Handler Configuration Builders. They contain already
    // default values for its variables, and others can be overwritten by child-classes

    // Basic configuration
    protected ProtocolBasicConfig.ProtocolBasicConfigBuilder basicConfigBuilder = ProtocolBasicConfig.builder();

    // Message Configuration Builder:
    protected MessageHandlerConfig.MessageHandlerConfigBuilder messageConfigBuilder
            = MessageHandlerConfig.builder();

    // Handshake Configuration Builder:
    protected HandshakeHandlerConfig.HandshakeHandlerConfigBuilder handshakeConfigBuilder
            = HandshakeHandlerConfig.builder();

    // Ping/Pong Configuration Builder:
    protected PingPongHandlerConfig.PingPongHandlerConfigBuilder pingPongConfigBuilder
            = PingPongHandlerConfig.builder();

    // Discovery Handler:
    protected DiscoveryHandlerConfig.DiscoveryHandlerConfigBuilder discoveryConfigBuilder
            = DiscoveryHandlerConfig.builder();

    // Blacklist Handler:
    protected BlacklistHandlerConfig.BlacklistHandlerConfigBuilder blacklistConfigBuilder
            = BlacklistHandlerConfig.builder();

    @Getter
    protected ProtocolBasicConfig basicConfig;

    // Stores all of the different Handlers Configurations:
    @Getter
    protected Map<String, HandlerConfig> handlersConfig = new HashMap<>();

    protected void build() {
        basicConfig = basicConfigBuilder.build();
        handlersConfig.put(MessageHandler.HANDLER_ID, messageConfigBuilder.basicConfig(basicConfig).build());
        handlersConfig.put(HandshakeHandler.HANDLER_ID, handshakeConfigBuilder.basicConfig(basicConfig).build());
        handlersConfig.put(PingPongHandler.HANDLER_ID, pingPongConfigBuilder.basicConfig(basicConfig).build());
        handlersConfig.put(DiscoveryHandler.HANDLER_ID, discoveryConfigBuilder.basicConfig(basicConfig).build());
        handlersConfig.put(BlacklistHandler.HANDLER_ID, blacklistConfigBuilder.basicConfig(basicConfig).build());
    }

    @Override
    public MessageHandlerConfig getMessageConfig() {
        return (MessageHandlerConfig) handlersConfig.get(MessageHandler.HANDLER_ID);
    }
    @Override
    public HandshakeHandlerConfig getHandshakeConfig() {
        return (HandshakeHandlerConfig) handlersConfig.get(HandshakeHandler.HANDLER_ID);
    }
    @Override
    public PingPongHandlerConfig getPingPongConfig() {
        return (PingPongHandlerConfig) handlersConfig.get(PingPongHandler.HANDLER_ID);
    }
    @Override
    public DiscoveryHandlerConfig  getDiscoveryConfig() {
        return (DiscoveryHandlerConfig) handlersConfig.get(DiscoveryHandler.HANDLER_ID);
    }
    @Override
    public BlacklistHandlerConfig  getBlacklistConfig() {
        return (BlacklistHandlerConfig) handlersConfig.get(BlacklistHandler.HANDLER_ID);
    }

    @Override
    public String toString() {
        return getId();
    }

}
