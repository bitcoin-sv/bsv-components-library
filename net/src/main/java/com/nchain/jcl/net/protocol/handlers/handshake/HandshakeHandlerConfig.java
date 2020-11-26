package com.nchain.jcl.net.protocol.handlers.handshake;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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

    private int servicesSupported;
    private String[] userAgentBlacklist;
    private String[] userAgentWhitelist;
    private long block_height;

    @Builder.Default
    private String userAgent = DEFAULT_USER_AGENT;
    @Builder.Default
    private boolean  relayTxs = false;



}
