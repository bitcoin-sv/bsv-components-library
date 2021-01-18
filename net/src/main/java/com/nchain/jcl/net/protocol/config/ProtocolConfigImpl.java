package com.nchain.jcl.net.protocol.config;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
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

    // Network Configuration: default
    @Getter
    @Builder.Default
    protected NetworkConfig networkConfig = new NetworkDefaultConfig();

    // Header of the Genesis Block of this Chain
    @Getter private BlockHeader genesisBlock;

    // We store some references to each Handler Configuration Builders. They contain already
    // default values for its variables, and others can be overwritten by child-classes

    @Getter protected ProtocolBasicConfig basicConfig;
    @Getter protected MessageHandlerConfig messageConfig;
    @Getter protected HandshakeHandlerConfig handshakeConfig;
    @Getter protected PingPongHandlerConfig pingPongConfig;
    @Getter protected DiscoveryHandlerConfig discoveryConfig;
    @Getter protected BlacklistHandlerConfig blacklistConfig;
    @Getter protected BlockDownloaderHandlerConfig blockDownloaderConfig;
    @Getter protected Map<String, HandlerConfig> handlersConfig = new HashMap<>();

    // Convenience parameters:
    Integer port;
    Integer minPeers;
    Integer maxPeers;

    @Builder(toBuilder = true)
    public ProtocolConfigImpl(
                                // Convenience parameters:
                                Integer port,
                                Integer minPeers,
                                Integer maxPeers,
                                // Genesis block:
                                BlockHeader genesisBlock,
                                // Handlers Configuration:
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

        this.basicConfig = basicConfig; // Mandatory. It cannot be null
        this.id = basicConfig.getId();


        // If some Convenience parameters hae been set, we use their values to overwrite default configurations:
        this.port = port;
        this.maxPeers = maxPeers;
        this.minPeers = minPeers;
        if (port != null)       this.basicConfig = this.basicConfig.toBuilder().port(port).build();
        if (minPeers != null)   this.basicConfig = this.basicConfig.toBuilder().minPeers(OptionalInt.of(minPeers)).build();
        if (maxPeers != null)   this.basicConfig = this.basicConfig.toBuilder().maxPeers(OptionalInt.of(maxPeers)).build();

        this.genesisBlock = genesisBlock;

        // For each one of the Handler Configurations;:
        // - If its not specified, we use the default one.
        // - Some of the properties of the Handler Configuration are included for convenience in the BasicConfiguration,
        //   in that case we overwrite them up...

        if (networkConfig != null) this.networkConfig = networkConfig;

        this.messageConfig = (messageConfig == null)
                ? MessageHandlerConfig.builder().basicConfig(this.basicConfig).build()
                : messageConfig.toBuilder().basicConfig(this.basicConfig).build();

        this.handshakeConfig = (handshakeConfig == null)
                ? new HandshakeHandlerConfig().toBuilder().basicConfig(this.basicConfig).build()
                : handshakeConfig.toBuilder().basicConfig(this.basicConfig).build();

        this.pingPongConfig = (pingPongConfig == null)
                ? new PingPongHandlerConfig().toBuilder().basicConfig(this.basicConfig).build()
                : pingPongConfig.toBuilder().basicConfig(this.basicConfig).build();

        this.discoveryConfig = (discoveryConfig == null)
                ? new DiscoveryHandlerConfig().toBuilder().basicConfig(this.basicConfig).build()
                : discoveryConfig.toBuilder().basicConfig(this.basicConfig).build();

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
