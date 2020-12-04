package com.nchain.jcl.net.protocol.config.provided;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.net.protocol.config.*;
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Stores the Configuration needed to run the Default P2P Handlers in the BTC Main Network.
 */
@Getter
public class ProtocolBTCMainConfig extends ProtocolConfigImpl implements ProtocolConfig {

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

    // Genesis Block for BTC-Main:
    private static BlockHeader genesisBlock = BlockHeader.builder()
            .version(1)
            .prevBlockHash(Sha256Wrapper.ZERO_HASH)
            .merkleRoot(Sha256Wrapper.wrap("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"))
            .difficultyTarget(0x1d00ffffL)
            .nonce(2083236893)
            .time(1231006505L)
            .numTxs(1)
            .build();

    // Basic Configuration:
    private static ProtocolBasicConfig basicConfig = ProtocolBasicConfig.builder()
            .id(id)
            .magicPackage(magicPackage)
            .port(port)
            .protocolVersion(protocolVersion)
            .build();

    /** Constructor */
    public ProtocolBTCMainConfig() {
        super( null,
                null,
                null,
                genesisBlock,
                basicConfig,
                null,            // Default Network Config
                null,           // Default Message Config
                HandshakeHandlerConfig.builder()
                        .userAgentBlacklist(userAgentBlacklist)
                        .userAgentWhitelist(userAgentWhitelist)
                        .servicesSupported(services)
                        .build(),
                null,           // Default PingPong Config
                DiscoveryHandlerConfig.builder()
                        .dns(dns)
                        .build(),
                null,            // Default Blacklist Config
                null);    // Default Block Downloader Config
    }

    @Override
    public String getId() { return id;}
}
