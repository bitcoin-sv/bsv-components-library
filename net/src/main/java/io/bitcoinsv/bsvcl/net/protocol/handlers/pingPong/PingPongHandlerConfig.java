package io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong;


import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.bsvcl.common.handlers.HandlerConfig;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the configuration variables needed by the PingPong Handler.
 */
public class PingPongHandlerConfig extends HandlerConfig {

    // Default Values:
    public static final Duration DEFAULT_INACTIVITY_TIMEOUT = Duration.ofSeconds(50);
    public static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(50);

    private ProtocolBasicConfig basicConfig;

    private Duration inactivityTimeout = DEFAULT_INACTIVITY_TIMEOUT;
    private Duration responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

    public PingPongHandlerConfig(ProtocolBasicConfig basicConfig, Duration inactivityTimeout, Duration responseTimeout) {
        this.basicConfig = basicConfig;
        if (inactivityTimeout != null)  this.inactivityTimeout = inactivityTimeout;
        if (responseTimeout != null)    this.responseTimeout = responseTimeout;
    }

    public PingPongHandlerConfig() {}

    public ProtocolBasicConfig getBasicConfig() { return this.basicConfig; }
    public Duration getInactivityTimeout()      { return this.inactivityTimeout; }
    public Duration getResponseTimeout()        { return this.responseTimeout; }

    public PingPongHandlerConfigBuilder toBuilder() {
        return new PingPongHandlerConfigBuilder().basicConfig(this.basicConfig).inactivityTimeout(this.inactivityTimeout).responseTimeout(this.responseTimeout);
    }

    public static PingPongHandlerConfigBuilder builder() {
        return new PingPongHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class PingPongHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;
        private Duration inactivityTimeout;
        private Duration responseTimeout;

        PingPongHandlerConfigBuilder() {}

        public PingPongHandlerConfig.PingPongHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public PingPongHandlerConfig.PingPongHandlerConfigBuilder inactivityTimeout(Duration inactivityTimeout) {
            this.inactivityTimeout = inactivityTimeout;
            return this;
        }

        public PingPongHandlerConfig.PingPongHandlerConfigBuilder responseTimeout(Duration responseTimeout) {
            this.responseTimeout = responseTimeout;
            return this;
        }

        public PingPongHandlerConfig build() {
            return new PingPongHandlerConfig(basicConfig, inactivityTimeout, responseTimeout);
        }
    }
}
