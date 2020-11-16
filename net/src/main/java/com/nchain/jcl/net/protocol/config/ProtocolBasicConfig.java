package com.nchain.jcl.net.protocol.config;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
    private int  port;

    @Builder.Default
    private int protocolVersion = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();
    @Builder.Default
    private OptionalInt maxPeers = OptionalInt.empty();
    @Builder.Default
    private OptionalInt minPeers = OptionalInt.empty();
}
