package com.nchain.jcl.net.protocol.handlers.pingPong;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the configuration variables needed by the PingPong Handler.
 */
public class PingPongHandlerConfig extends HandlerConfig {

    // Default Values:
    public static final Long DEFAULT_INACTIVITY_TIMEOUT = Duration.ofMinutes(4).toMillis();
    public static final Long DEFAULT_RESPONSE_TIMEOUT = Duration.ofMinutes(3).toMillis();

    private ProtocolBasicConfig basicConfig;

    private long inactivityTimeout = DEFAULT_INACTIVITY_TIMEOUT;
    private long responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

    public PingPongHandlerConfig(ProtocolBasicConfig basicConfig, Long inactivityTimeout, Long responseTimeout) {
        this.basicConfig = basicConfig;
        if (inactivityTimeout != null)  this.inactivityTimeout = inactivityTimeout;
        if (responseTimeout != null)    this.responseTimeout = responseTimeout;
    }

    public PingPongHandlerConfig() {}

    public ProtocolBasicConfig getBasicConfig() { return this.basicConfig; }
    public long getInactivityTimeout()          { return this.inactivityTimeout; }
    public long getResponseTimeout()            { return this.responseTimeout; }

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
        private long inactivityTimeout;
        private long responseTimeout;

        PingPongHandlerConfigBuilder() {}

        public PingPongHandlerConfig.PingPongHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public PingPongHandlerConfig.PingPongHandlerConfigBuilder inactivityTimeout(long inactivityTimeout) {
            this.inactivityTimeout = inactivityTimeout;
            return this;
        }

        public PingPongHandlerConfig.PingPongHandlerConfigBuilder responseTimeout(long responseTimeout) {
            this.responseTimeout = responseTimeout;
            return this;
        }

        public PingPongHandlerConfig build() {
            return new PingPongHandlerConfig(basicConfig, inactivityTimeout, responseTimeout);
        }
    }
}
