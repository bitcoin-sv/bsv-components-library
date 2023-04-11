package io.bitcoinsv.bsvcl.net.protocol.handlers.handshake;


import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolServices;
import io.bitcoinsv.bsvcl.common.handlers.HandlerConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the configuration variables needed by the Handshake Handler
 */
public class HandshakeHandlerConfig extends HandlerConfig {

    // Default Values:
    public static final String DEFAULT_USER_AGENT       = "/bitcoin-bsvcl:" + "1.0.0" + "/";
    public static String[] DEFAULT_USER_AGENT_BLACKLIST = new String[] {"Bitcoin ABC", "BUCash", "Bitcoin Cash", "BCH Unlimited", "bchd:" };

    // NOTE: We empty the WHITELIST, so we accept any incoming connection (if its arealdy passed the AGENT_BLACKLIST validation)
    public static String[] DEFAULT_USER_AGENT_WHITELIST = new String[0];

    public static int DEFAULt_SERVICES                  = ProtocolServices.NODE_BLOOM.getProtocolServices();

    private ProtocolBasicConfig basicConfig;

    private int servicesSupported = DEFAULt_SERVICES;
    private long block_height = 0;
    private String[] userAgentBlacklist = DEFAULT_USER_AGENT_BLACKLIST;
    private String[] userAgentWhitelist = DEFAULT_USER_AGENT_WHITELIST;
    private String userAgent = DEFAULT_USER_AGENT;
    private boolean  relayTxs = false;

    public HandshakeHandlerConfig(ProtocolBasicConfig basicConfig, Integer servicesSupported, Long block_height, String[] userAgentBlacklist, String[] userAgentWhitelist, String userAgent, boolean relayTxs) {
        this.basicConfig = basicConfig;
        if (servicesSupported != null)  this.servicesSupported = servicesSupported;
        if (block_height != null)       this.block_height = block_height;
        if (userAgentBlacklist != null) this.userAgentBlacklist = userAgentBlacklist;
        if (userAgentWhitelist != null) this.userAgentWhitelist = userAgentWhitelist;
        if (userAgent != null)          this.userAgent = userAgent;
        this.relayTxs = relayTxs;
    }

    public HandshakeHandlerConfig() { }

    public ProtocolBasicConfig getBasicConfig() { return this.basicConfig; }
    public int getServicesSupported()           { return this.servicesSupported; }
    public long getBlock_height()               { return this.block_height; }
    public String[] getUserAgentBlacklist()     { return this.userAgentBlacklist; }
    public String[] getUserAgentWhitelist()     { return this.userAgentWhitelist; }
    public String getUserAgent()                { return this.userAgent; }
    public boolean isRelayTxs()                 { return this.relayTxs; }

    public HandshakeHandlerConfigBuilder toBuilder() {
        return new HandshakeHandlerConfigBuilder().basicConfig(this.basicConfig).servicesSupported(this.servicesSupported).block_height(this.block_height).userAgentBlacklist(this.userAgentBlacklist).userAgentWhitelist(this.userAgentWhitelist).userAgent(this.userAgent).relayTxs(this.relayTxs);
    }

    public static HandshakeHandlerConfigBuilder builder() {
        return new HandshakeHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class HandshakeHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;
        private Integer servicesSupported;
        private long block_height;
        private String[] userAgentBlacklist;
        private String[] userAgentWhitelist;
        private String userAgent;
        private boolean relayTxs;

        HandshakeHandlerConfigBuilder() {}

        public HandshakeHandlerConfig.HandshakeHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public HandshakeHandlerConfig.HandshakeHandlerConfigBuilder servicesSupported(Integer servicesSupported) {
            this.servicesSupported = servicesSupported;
            return this;
        }

        public HandshakeHandlerConfig.HandshakeHandlerConfigBuilder block_height(long block_height) {
            this.block_height = block_height;
            return this;
        }

        public HandshakeHandlerConfig.HandshakeHandlerConfigBuilder userAgentBlacklist(String[] userAgentBlacklist) {
            this.userAgentBlacklist = userAgentBlacklist;
            return this;
        }

        public HandshakeHandlerConfig.HandshakeHandlerConfigBuilder userAgentWhitelist(String[] userAgentWhitelist) {
            this.userAgentWhitelist = userAgentWhitelist;
            return this;
        }

        public HandshakeHandlerConfig.HandshakeHandlerConfigBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public HandshakeHandlerConfig.HandshakeHandlerConfigBuilder relayTxs(boolean relayTxs) {
            this.relayTxs = relayTxs;
            return this;
        }

        public HandshakeHandlerConfig build() {
            return new HandshakeHandlerConfig(basicConfig, servicesSupported, block_height, userAgentBlacklist, userAgentWhitelist, userAgent, relayTxs);
        }
    }
}
