package com.nchain.jcl.net.protocol.handlers.pingPong;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the configuration variables needed by the PingPong Handler.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PingPongHandlerConfig extends HandlerConfig {

    // Default Values:
    public static final Long DEFAULT_INACTIVITY_TIMEOUT = Duration.ofMinutes(4).toMillis();
    public static final Long DEFAULT_RESPONSE_TIMEOUT = Duration.ofMinutes(3).toMillis();

    private ProtocolBasicConfig basicConfig;

    @Builder.Default
    private long inactivityTimeout = DEFAULT_INACTIVITY_TIMEOUT;
    @Builder.Default
    private long responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
}
