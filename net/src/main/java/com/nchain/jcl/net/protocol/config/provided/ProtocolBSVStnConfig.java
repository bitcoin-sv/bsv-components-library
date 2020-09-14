package com.nchain.jcl.net.protocol.config.provided;

import com.nchain.jcl.net.protocol.config.*;
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import lombok.Getter;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 12:04
 *
 * Stores the Configuration needed to run the Default P2P Handlers in the BSV STN Network.
 */
@Getter
public class ProtocolBSVStnConfig extends ProtocolConfigImpl implements ProtocolConfig {

    // Specific values for BSV STN Net:

    private static String id                   = "BSV [stn Net]";
    private static long magicPackage           = 0xf9c4cefbL;
    private static int services                = ProtocolServices.NODE_BLOOM.getProtocolServices();
    private static int port                    = 9333;
    private static int protocolVersion         = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();

    private static String[] userAgentBlacklist = new String[] {"Bitcoin ABC:", "BUCash:" };
    private static String[] userAgentWhitelist = new String[] {"Bitcoin SV:", HandshakeHandlerConfig.DEFAULT_USER_AGENT };

    private static String[] dns                = new String[] {
            "stn-seed.bitcoinsv.io"
    };


    /** Constructor */
    public ProtocolBSVStnConfig() {
        super(
                id,
                magicPackage,
                services,
                port,
                protocolVersion,
                userAgentBlacklist,
                userAgentWhitelist,
                dns,
                null,    // minPeers not specified
                null,   // maxPeers not specified
                new ProtocolBasicConfig().toBuilder()
                        .id(id)
                        .magicPackage(magicPackage)
                        .servicesSupported(services)
                        .port(port)
                        .protocolVersion(protocolVersion)
                        .build(),
                null,               // Default Network Config
                null,              // Default Message Config
                new HandshakeHandlerConfig().toBuilder()
                        .userAgentBlacklistPatterns(userAgentBlacklist)
                        .userAgentWhitelistPatterns(userAgentWhitelist)
                        .build(),
                null,             // Default PingPong Config
                new DiscoveryHandlerConfig().toBuilder()
                        .dnsSeeds(dns)
                        .build(),
                null,              // Default Blacklist Config
                null);      // Default Block Downloader Config
    }

    @Override
    public String getId() { return id;}
}
