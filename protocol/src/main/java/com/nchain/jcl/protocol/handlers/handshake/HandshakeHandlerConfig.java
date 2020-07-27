package com.nchain.jcl.protocol.handlers.handshake;

import com.nchain.jcl.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 11:42
 *
 * It stores the configuration variables needed by the Handshake Handler
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class HandshakeHandlerConfig extends HandlerConfig {

    public static final String DEFAULT_USER_AGENT = "/bitcoinj-sv:" + "0.0.7" + "/";

    private ProtocolBasicConfig basicConfig;

    @Builder.Default
    private String userAgent = DEFAULT_USER_AGENT;
    @Builder.Default
    private boolean  relayTxs = false;
    @Builder.Default
    private OptionalInt maxPeers = OptionalInt.empty();
    @Builder.Default
    private OptionalInt minPeers = OptionalInt.empty();

    private String[] userAgentBlacklistPatterns;
    private String[] userAgentWhitelistPatterns;

}
