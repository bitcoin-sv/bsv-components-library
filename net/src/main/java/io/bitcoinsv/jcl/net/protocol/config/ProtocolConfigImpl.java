package io.bitcoinsv.jcl.net.protocol.config;


import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandler;
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */

public class ProtocolConfigImpl implements ProtocolConfig {

    protected String id;

    // Header of the Genesis Block of this Chain
    private BlockHeaderMsg genesisBlock;

    // We store some references to each Handler Configuration Builders. They contain already
    // default values for its variables, and others can be overwritten by child-classes

    protected ProtocolBasicConfig basicConfig;
    protected MessageHandlerConfig messageConfig;
    protected HandshakeHandlerConfig handshakeConfig;
    protected PingPongHandlerConfig pingPongConfig;
    protected DiscoveryHandlerConfig discoveryConfig;
    protected BlacklistHandlerConfig blacklistConfig;
    protected BlockDownloaderHandlerConfig blockDownloaderConfig;
    protected Map<String, HandlerConfig> handlersConfig = new HashMap<>();

    // Convenience parameters:
    Integer port;
    Integer minPeers;
    Integer maxPeers;

    public ProtocolConfigImpl(
                                // Convenience parameters:
                                Integer port,
                                Integer minPeers,
                                Integer maxPeers,
                                // Genesis block:
                                BlockHeaderMsg genesisBlock,
                                // Handlers Configuration:
                                ProtocolBasicConfig basicConfig,
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

    public String getId()                                           { return this.id; }
    public BlockHeaderMsg getGenesisBlock()                         { return this.genesisBlock; }
    public ProtocolBasicConfig getBasicConfig()                     { return this.basicConfig; }
    public MessageHandlerConfig getMessageConfig()                  { return this.messageConfig; }
    public HandshakeHandlerConfig getHandshakeConfig()              { return this.handshakeConfig; }
    public PingPongHandlerConfig getPingPongConfig()                { return this.pingPongConfig; }
    public DiscoveryHandlerConfig getDiscoveryConfig()              { return this.discoveryConfig; }
    public BlacklistHandlerConfig getBlacklistConfig()              { return this.blacklistConfig; }
    public BlockDownloaderHandlerConfig getBlockDownloaderConfig()  { return this.blockDownloaderConfig; }
    public Map<String, HandlerConfig> getHandlersConfig()           { return this.handlersConfig; }

    public static ProtocolConfigImplBuilder builder() {
        return new ProtocolConfigImplBuilder();
    }

    public ProtocolConfigImplBuilder toBuilder() {
        return new ProtocolConfigImplBuilder()
                .port(this.port)
                .minPeers(this.minPeers)
                .maxPeers(this.maxPeers)
                .genesisBlock(this.genesisBlock)
                .basicConfig(this.basicConfig)
                .messageConfig(this.messageConfig)
                .handshakeConfig(this.handshakeConfig)
                .pingPongConfig(this.pingPongConfig)
                .discoveryConfig(this.discoveryConfig)
                .blacklistConfig(this.blacklistConfig)
                .blockDownloaderConfig(this.blockDownloaderConfig);
    }

    /**
     * Builder
     */
    public static class ProtocolConfigImplBuilder {
        private Integer port;
        private Integer minPeers;
        private Integer maxPeers;
        private BlockHeaderMsg genesisBlock;
        private ProtocolBasicConfig basicConfig;
        private MessageHandlerConfig messageConfig;
        private HandshakeHandlerConfig handshakeConfig;
        private PingPongHandlerConfig pingPongConfig;
        private DiscoveryHandlerConfig discoveryConfig;
        private BlacklistHandlerConfig blacklistConfig;
        private BlockDownloaderHandlerConfig blockDownloaderConfig;

        ProtocolConfigImplBuilder() {}

        public ProtocolConfigImpl.ProtocolConfigImplBuilder port(Integer port) {
            this.port = port;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder minPeers(Integer minPeers) {
            this.minPeers = minPeers;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder maxPeers(Integer maxPeers) {
            this.maxPeers = maxPeers;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder genesisBlock(BlockHeaderMsg genesisBlock) {
            this.genesisBlock = genesisBlock;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder messageConfig(MessageHandlerConfig messageConfig) {
            this.messageConfig = messageConfig;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder handshakeConfig(HandshakeHandlerConfig handshakeConfig) {
            this.handshakeConfig = handshakeConfig;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder pingPongConfig(PingPongHandlerConfig pingPongConfig) {
            this.pingPongConfig = pingPongConfig;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder discoveryConfig(DiscoveryHandlerConfig discoveryConfig) {
            this.discoveryConfig = discoveryConfig;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder blacklistConfig(BlacklistHandlerConfig blacklistConfig) {
            this.blacklistConfig = blacklistConfig;
            return this;
        }

        public ProtocolConfigImpl.ProtocolConfigImplBuilder blockDownloaderConfig(BlockDownloaderHandlerConfig blockDownloaderConfig) {
            this.blockDownloaderConfig = blockDownloaderConfig;
            return this;
        }

        public ProtocolConfigImpl build() {
            return new ProtocolConfigImpl(port, minPeers, maxPeers, genesisBlock, basicConfig, messageConfig, handshakeConfig, pingPongConfig, discoveryConfig, blacklistConfig, blockDownloaderConfig);
        }
    }
}
