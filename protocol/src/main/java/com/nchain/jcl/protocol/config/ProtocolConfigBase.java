package com.nchain.jcl.protocol.config;

import com.nchain.jcl.network.config.NetworkConfig;
import com.nchain.jcl.network.config.NetworkConfigDefault;
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandler;
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandlerConfig;
import com.nchain.jcl.protocol.handlers.block.BlockDownloaderHandler;
import com.nchain.jcl.protocol.handlers.block.BlockDownloaderHandlerConfig;
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandler;
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.protocol.handlers.handshake.HandshakeHandler;
import com.nchain.jcl.protocol.handlers.handshake.HandshakeHandlerConfig;
import com.nchain.jcl.protocol.handlers.message.MessageHandler;
import com.nchain.jcl.protocol.handlers.message.MessageHandlerConfig;
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandler;
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandlerConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-08-18
 */

public class ProtocolConfigBase implements ProtocolConfig {

    @Getter
    protected String id;

    // Individual properties that are convenient to have them separately, so they can be set/changed by the
    // builder. Some of these properties might also be contained in some of the Handler Configurations.

    private Long magicPackage;
    private Integer services;
    private Integer port;
    private Integer protocolVersion;
    private String[] userAgentBlacklist;
    private String[] userAgentWhitelist;
    private String[] dns;

    // Network Configuration: default
    @Getter
    @Builder.Default
    protected NetworkConfig networkConfig = new NetworkConfigDefault();

    // We store some references to each Handler Configuration Builders. They contain already
    // default values for its variables, and others can be overwritten by child-classes

    // Basic configuration
    @Getter
    protected ProtocolBasicConfig basicConfig;

    // Message Configuration
    @Getter
    protected MessageHandlerConfig messageConfig;

    // Handshake Configuration
    @Getter
    protected HandshakeHandlerConfig handshakeConfig;

    // Ping/Pong Configuration Builder:
    @Getter
    protected PingPongHandlerConfig pingPongConfig;

    // Discovery Handler:
    @Getter
    protected DiscoveryHandlerConfig discoveryConfig;

    // Blacklist Handler:
    @Getter
    protected BlacklistHandlerConfig blacklistConfig;

    // Block downloader Config
    @Getter
    protected BlockDownloaderHandlerConfig blockDownloaderConfig;

    // Stores all of the different Handlers Configurations:
    @Getter
    protected Map<String, HandlerConfig> handlersConfig = new HashMap<>();

    @Builder(toBuilder = true)
    public ProtocolConfigBase(
                              // Convenience Properties:
                              String id,
                              Long magicPackage,
                              Integer services,
                              Integer port,
                              Integer protocolVersion,
                              String[] userAgentBlacklist,
                              String[] userAgentWhitelist,
                              String[] dns,

                              // Handler Configurations:
                              ProtocolBasicConfig basicConfig,
                              NetworkConfig networkConfig,
                              MessageHandlerConfig messageConfig,
                              HandshakeHandlerConfig handshakeConfig,
                              PingPongHandlerConfig pingPongConfig,
                              DiscoveryHandlerConfig discoveryConfig,
                              BlacklistHandlerConfig blacklistConfig,
                              BlockDownloaderHandlerConfig blockDownloaderConfig) {

        // The Basic Config is Mandatory...
        checkArgument(basicConfig != null, "The Basic Config is mandatory");

        this.id = id;
        if (networkConfig != null) this.networkConfig = networkConfig;

        // For each one of the Handler Configurations;:
        // - If its not specified, we use the default one.
        // - If some of the Convenience properties are set and are also contained in the Handler Configuration, the
        //   property inside the Handler configuration is overwritten

        this.basicConfig = basicConfig; // Mandatory. It cannot be null
        if (id != null)                 this.basicConfig = basicConfig.toBuilder().id(id).build();
        if (magicPackage != null)       this.basicConfig = basicConfig.toBuilder().magicPackage(magicPackage).build();
        if (services != null)           this.basicConfig = basicConfig.toBuilder().servicesSupported(services).build();
        if (port != null)               this.basicConfig = basicConfig.toBuilder().port(port).build();
        if (protocolVersion != null)    this.basicConfig = basicConfig.toBuilder().protocolVersion(protocolVersion).build();

        this.messageConfig = (messageConfig == null)
                ? new MessageHandlerConfig(this.basicConfig).toBuilder().build()
                : messageConfig.toBuilder().basicConfig(this.basicConfig).build();

        this.handshakeConfig = (handshakeConfig == null)
                ? new HandshakeHandlerConfig().toBuilder().basicConfig(this.basicConfig).build()
                : handshakeConfig.toBuilder().basicConfig(this.basicConfig).build();
        if (userAgentBlacklist != null && userAgentBlacklist.length > 0)
            this.handshakeConfig = this.handshakeConfig.toBuilder().userAgentBlacklistPatterns(userAgentBlacklist).build();
        if (userAgentWhitelist != null && userAgentWhitelist.length > 0)
            this.handshakeConfig = this.handshakeConfig.toBuilder().userAgentWhitelistPatterns(userAgentWhitelist).build();

        this.pingPongConfig = (pingPongConfig == null)
                ? new PingPongHandlerConfig().toBuilder().basicConfig(this.basicConfig).build()
                : pingPongConfig.toBuilder().basicConfig(this.basicConfig).build();

        this.discoveryConfig = (discoveryConfig == null)
                ? new DiscoveryHandlerConfig().toBuilder().basicConfig(this.basicConfig).build()
                : discoveryConfig.toBuilder().basicConfig(this.basicConfig).build();
        if (dns != null && dns.length > 0)
            this.discoveryConfig = this.discoveryConfig.toBuilder().dnsSeeds(dns).build();

        this.blacklistConfig = (blacklistConfig == null)
                ? new BlacklistHandlerConfig(this.basicConfig).toBuilder().build()
                : blacklistConfig.toBuilder().basicConfig(this.basicConfig).build();

        this.blockDownloaderConfig = (blockDownloaderConfig == null)
                ? new BlockDownloaderHandlerConfig().toBuilder().basicConfig(this.basicConfig).build()
                : blockDownloaderConfig.toBuilder().basicConfig(this.basicConfig).build();


        handlersConfig.put(MessageHandler.HANDLER_ID, this.messageConfig);
        handlersConfig.put(HandshakeHandler.HANDLER_ID, this.handshakeConfig);
        handlersConfig.put(PingPongHandler.HANDLER_ID, this.pingPongConfig);
        handlersConfig.put(DiscoveryHandler.HANDLER_ID, this.discoveryConfig);
        handlersConfig.put(BlacklistHandler.HANDLER_ID, this.blacklistConfig);
        handlersConfig.put(BlockDownloaderHandler.HANDLER_ID, this.blockDownloaderConfig);
    }
}
