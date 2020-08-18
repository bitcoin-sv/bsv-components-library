package com.nchain.jcl.protocol.config.provided;

import com.nchain.jcl.protocol.config.*;
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.protocol.handlers.handshake.HandshakeHandlerConfig;
import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 12:04
 *
 * Stores the Configuration needed to run the Default P2P Handlers in the BTC Main Network.
 */
@Getter
public class ProtocolBTCMainConfig extends ProtocolConfigBase implements ProtocolConfig {

    // Specific values for BTC main Net:

    private static String id                   = "BTC [main Net]";
    private static long magicPackage           = 0xd9b4bef9L;
    private static int services                = ProtocolServices.NODE_BLOOM.getProtocolServices();
    private static int port                    = 8333;
    private static int protocolVersion         = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();

    private static String[] userAgentBlacklist = new String[] {"Bitcoin SV:", "BUCash:" };
    private static String[] userAgentWhitelist = null;

    private static String[] dns                = new String[] {
            "seed.bitcoin.sipa.be",         // Pieter Wuille
            "dnsseed.bluematt.me",          // Matt Corallo
            "dnsseed.bitcoin.dashjr.org",   // Luke Dashjr
            "seed.bitcoinstats.com",        // Chris Decker
            "seed.bitcoin.jonasschnelli.ch",// Jonas Schnelli
            "seed.btc.petertodd.org",       // Peter Todd
            "seed.bitcoin.sprovoost.nl",    // Sjors Provoost
            "dnsseed.emzy.de",              // Stephan Oeste
    };


    /** Constructor */
    public ProtocolBTCMainConfig() {
        super(
                id,
                magicPackage,
                services,
                port,
                protocolVersion,
                userAgentBlacklist,
                userAgentWhitelist,
                dns,
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
