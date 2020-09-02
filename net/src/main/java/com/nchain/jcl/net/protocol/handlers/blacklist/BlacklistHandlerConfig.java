package com.nchain.jcl.net.protocol.handlers.blacklist;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 11:49
 *
 * It stores the configuration needed by the Blacklist Handler
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlacklistHandlerConfig extends HandlerConfig {
    private ProtocolBasicConfig basicConfig;
}
