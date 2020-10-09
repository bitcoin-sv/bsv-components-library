package com.nchain.jcl.net.protocol.config;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.network.config.NetworkConfig;
import com.nchain.jcl.net.network.config.provided.NetworkDefaultConfig;
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandler;
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandler;
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandler;
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandler;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.message.MessageHandler;
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandler;
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandlerConfig;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */

public class ProtocolConfigImpl implements ProtocolConfig {

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
    private Integer minPeers;
    private Integer maxPeers;

    // Network Configuration: default
    @Getter
    @Builder.Default
    protected NetworkConfig networkConfig = new NetworkDefaultConfig();

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
    public ProtocolConfigImpl(
                              // Convenience Properties:
                              String id,
                              Long magicPackage,
                              Integer services,
                              Integer port,
                              Integer protocolVersion,
                              String[] userAgentBlacklist,
                              String[] userAgentWhitelist,
                              String[] dns,
                              Integer minPeers,
                              Integer maxPeers,

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
        if (minPeers != null)
            this.handshakeConfig = this.handshakeConfig.toBuilder().minPeers(OptionalInt.of(minPeers)).build();
        if (maxPeers != null)
            this.handshakeConfig = this.handshakeConfig.toBuilder().maxPeers(OptionalInt.of(maxPeers)).build();

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

    @Override
    public String toString() { return getId();}
}
