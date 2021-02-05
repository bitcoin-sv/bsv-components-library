package com.nchain.jcl.net.protocol.handlers.blacklist;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the configuration needed by the Blacklist Handler
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlacklistHandlerConfig extends HandlerConfig {
    private ProtocolBasicConfig basicConfig;
}
