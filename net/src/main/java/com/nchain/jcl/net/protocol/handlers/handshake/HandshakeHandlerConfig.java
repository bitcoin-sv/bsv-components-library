package com.nchain.jcl.net.protocol.handlers.handshake;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.config.ProtocolServices;
import com.nchain.jcl.tools.handlers.HandlerConfig;
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

    // Default Values:
    public static final String DEFAULT_USER_AGENT       = "/bitcoinj-sv:" + "0.0.7" + "/";
    public static String[] DEFAULT_USER_AGENT_BLACKLIST = new String[] {"Bitcoin ABC:", "BUCash:" };
    public static String[] DEFAULT_USER_AGENT_WHITELIST = new String[] {"Bitcoin SV:", HandshakeHandlerConfig.DEFAULT_USER_AGENT };
    public static int DEFAULt_SERVICES                  = ProtocolServices.NODE_BLOOM.getProtocolServices();

    private ProtocolBasicConfig basicConfig;

    @Builder.Default private int servicesSupported = DEFAULt_SERVICES;
    @Builder.Default private long block_height = 0;
    @Builder.Default private String[] userAgentBlacklist = DEFAULT_USER_AGENT_BLACKLIST;
    @Builder.Default private String[] userAgentWhitelist = DEFAULT_USER_AGENT_WHITELIST;
    @Builder.Default private String userAgent = DEFAULT_USER_AGENT;
    @Builder.Default private boolean  relayTxs = false;

}
