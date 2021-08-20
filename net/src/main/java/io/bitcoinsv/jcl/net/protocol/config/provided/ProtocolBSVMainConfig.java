/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.config.provided;



import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigImpl;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion;
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Stores the Configuration needed to run the Default P2P Handlers in the BSV Main Network.
 */
/*
    This class is DEPRECATED.
    Pre-defined Protocol Configuration instances are now obtained by using the ProtocolConfigBuilder and the
    NetworkParam classes from BitcoinJ.
    This class is still here because some of its parameters are different from te ones in BitcoinJ, like the list
    of DNS. Since these values might make some difference in terms of performance, we keep it for future reference.
 */
@Deprecated
public class ProtocolBSVMainConfig extends ProtocolConfigImpl implements ProtocolConfig {

    // Specific values for BSV Main Net:

    public static String id                   = "BSV [main Net]";
    private static long magicPackage           = 0xe8f3e1e3L;
    //private static int services                = ProtocolServices.NODE_BLOOM.getProtocolServices();
    private static int services                = 0;
    private static int port                    = 8333;
    private static int protocolVersion         = ProtocolVersion.CURRENT.getBitcoinProtocolVersion();
    private static String[] userAgentBlacklist = new String[] {"Bitcoin ABC:", "BUCash:" };
    private static String[] userAgentWhitelist = new String[] {"Bitcoin SV:", HandshakeHandlerConfig.DEFAULT_USER_AGENT };
    private static String[] dns                = new String[] {
            "seed.bitcoinsv.io",
            "seed.cascharia.com",
            "seed.satoshivision.network"
    };

    // Genesis Block for BSV-Main:
    public static BlockHeaderMsg genesisBlock = BlockHeaderMsg.builder()
            .version(1)
            .prevBlockHash(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
            .merkleRoot(HashMsg.builder().hash(Sha256Hash.wrap("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b").getBytes()).build())
            .difficultyTarget(0x1d00ffffL)
            .nonce(2083236893)
            .creationTimestamp(1231006505L)
            .build();

    // Basic Configuration:
    private static ProtocolBasicConfig basicConfig = ProtocolBasicConfig.builder()
            .id(id)
            .magicPackage(magicPackage)
            .port(port)
            .protocolVersion(protocolVersion)
            .build();

    /** Constructor */
    public ProtocolBSVMainConfig() {
        super( null,
                null,
                null,
                genesisBlock,
                basicConfig,
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

}
