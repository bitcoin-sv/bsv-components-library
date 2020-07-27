package com.nchain.jcl.protocol.config;

import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 11:37
 *
 * This class stores some basic variables. they are the ones suitable to be changed when we changed from one
 * network to a different one.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolBasicConfig extends HandlerConfig {
    private String id;
    private long magicPackage;
    private int  servicesSupported;
    private int  port;
    @Builder.Default
    private int protocolVersion = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();
}
