package com.nchain.jcl.protocol.config.provided;


import com.nchain.jcl.protocol.config.ProtocolServices;
import com.nchain.jcl.protocol.config.ProtocolConfig;
import com.nchain.jcl.protocol.config.DefaultHandlersConfig;
import com.nchain.jcl.protocol.config.ProtocolVersion;
import com.nchain.jcl.protocol.handlers.handshake.HandshakeHandlerConfig;
import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 12:04
 *
 * Stores the Configuration needed to run the Default P2P Handlers in the BSV Main Network.
 */
@Getter
public class ProtocolBSVMainConfig extends DefaultHandlersConfig implements ProtocolConfig {

    // Specific values for BSV Main Net:

    @Builder.Default private static String id                   = "BSV [main Net]";
    @Builder.Default private static long magicPackage           = 0xe8f3e1e3L;
    @Builder.Default private static int services                = ProtocolServices.NODE_BLOOM.getProtocolServices();
    @Builder.Default private static int port                    = 8333;
    @Builder.Default private static int protocolVersion         = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();
    @Builder.Default private static String[] userAgentBlacklist = new String[] {"Bitcoin ABC:", "BUCash:" };
    @Builder.Default private static String[] userAgentWhitelist = new String[] {"Bitcoin SV:", HandshakeHandlerConfig.DEFAULT_USER_AGENT };
    @Builder.Default private static String[] dns                = new String[] {
            "seed.bitcoinsv.io",
            "seed.cascharia.com",
            "seed.satoshivision.network"
    };

    /** Constructor */
    public ProtocolBSVMainConfig() {
        this(id, magicPackage, services, port, protocolVersion, userAgentBlacklist, userAgentWhitelist, dns);
    }

    @Builder(toBuilder = true)
    public ProtocolBSVMainConfig(String id, long magicPackage, int services, int port, int protocolVersion,
                                 String[] userAgentBlacklist, String[] userAgentWhitelist, String[] dns) {
        basicConfigBuilder
                .id(id)
                .magicPackage(magicPackage) // sent over the wire as e3e1f3e8L;
                .servicesSupported(services)
                .port(port)
                .protocolVersion(protocolVersion);
        handshakeConfigBuilder
                .userAgentBlacklistPatterns(userAgentBlacklist)
                .userAgentWhitelistPatterns(userAgentWhitelist);
        discoveryConfigBuilder.dnsSeeds(dns);
        super.build();
    }

    @Override
    public String getId() { return id;}
}
