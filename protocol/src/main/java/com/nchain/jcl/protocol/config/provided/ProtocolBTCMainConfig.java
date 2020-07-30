package com.nchain.jcl.protocol.config.provided;

import com.nchain.jcl.protocol.config.ProtocolServices;
import com.nchain.jcl.protocol.config.DefaultHandlersConfig;
import com.nchain.jcl.protocol.config.ProtocolConfig;
import com.nchain.jcl.protocol.config.ProtocolVersion;
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
@Builder(toBuilder = true)
public class ProtocolBTCMainConfig extends DefaultHandlersConfig implements ProtocolConfig {

    // Specific values for BTC main Net:

    @Builder.Default private static String id                   = "BTC [main Net]";
    @Builder.Default private static long magicPackage           = 0xd9b4bef9L;
    @Builder.Default private static int services                = ProtocolServices.NODE_BLOOM.getProtocolServices();
    @Builder.Default private static int port                    = 8333;
    @Builder.Default private static int protocolVersion         = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();

    @Builder.Default private static String[] userAgentBlacklist = new String[] {"Bitcoin SV:", "BUCash:" };
    @Builder.Default private static String[] userAgentWhitelist = null;

    @Builder.Default private static String[] dns                = new String[] {
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
        this(id, magicPackage, services, port, protocolVersion, userAgentBlacklist, userAgentWhitelist, dns);
    }

    @Builder(toBuilder = true)
    public ProtocolBTCMainConfig(String id, long magicPackage, int services, int port, int protocolVersion,
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
        // in BTC, a lot of times we cannot download a block for any reason, so we increase the max Attemps. It's ok,
        // since the blocks are small (<= 1MB), so the performance is not affected.
        blockConfigBuilder.maxDownloadAttempts(10);

        super.build();
    }

    @Override
    public String getId() { return id;}
}
