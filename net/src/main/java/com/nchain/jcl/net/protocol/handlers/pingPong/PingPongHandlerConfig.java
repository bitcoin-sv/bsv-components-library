package com.nchain.jcl.net.protocol.handlers.pingPong;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private ProtocolBasicConfig basicConfig;

    @Builder.Default
    private long inactivityTimeout = 240000; // 3 minutes
    @Builder.Default
    private long responseTimeout = 180000; // 2 minutes;
}
