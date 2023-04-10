package io.bitcoinsv.bsvcl.net.protocol.handlers.whitelist;


import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.bsvcl.tools.handlers.HandlerConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the configuration needed by the Whitelist Handler
 */
public class WhitelistHandlerConfig extends HandlerConfig {
    private ProtocolBasicConfig basicConfig;

    public WhitelistHandlerConfig(ProtocolBasicConfig basicConfig) {
        this.basicConfig = basicConfig;
    }

    public ProtocolBasicConfig getBasicConfig() {
        return this.basicConfig;
    }

    public WhitelistHandlerConfigBuilder toBuilder() {
        return new WhitelistHandlerConfigBuilder().basicConfig(this.basicConfig);
    }

    public static WhitelistHandlerConfigBuilder builder() {
        return new WhitelistHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class WhitelistHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;

        WhitelistHandlerConfigBuilder() {}

        public WhitelistHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public WhitelistHandlerConfig build() {
            return new WhitelistHandlerConfig(basicConfig);
        }
    }
}
