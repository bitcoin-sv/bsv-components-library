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

    // Genesis Block for BSV-STN:
    private static BlockHeader genesisBlock = BlockHeader.builder()
            .version(1)
            .prevBlockHash(Sha256Wrapper.ZERO_HASH)
            .merkleRoot(Sha256Wrapper.wrap("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"))
            .difficultyTarget(0x1d00ffffL)
            .nonce(414098458)
            .time(1296688602L)
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
    public ProtocolBSVStnConfig() {
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
