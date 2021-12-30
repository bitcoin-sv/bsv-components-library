package io.bitcoinsv.jcl.net.protocol.config;

import com.google.common.primitives.Longs;
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bitcoinjsv.params.NetworkParameters;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-02-03
 *
 * This class takes a NetworkParams class frm bitcoinJ and generates a ProtocolConfig class, which can then be
 * used with the JCL P2P Service.
 */
public class ProtocolConfigBuilder {

    // Default Values:
    private static final int MIN_PEERS_DEFAULT = 10;
    private static final int MAX_PEERS_DEFAULT = 15;

    /*
        In BitcoinJ, the magic package is specified using a different Order than in JCL.
        for example, the magic package field in the messages headers is specified this way:
            - 0xe8f3e1e3L in JCL (same order as is sent over the wire)
            - 0xe3e1f3e8L in BitcoinJ

        This function translates the value from BitcoinJ into the equivalente representation using JCL
     */
    private static long convertMagicPackageFronBitcoinJ(long magic) {
        byte[] result = new byte[8];
        Utils.uint32ToByteArrayBE(magic, result, 0);
        return Longs.fromByteArray(Utils.reverseBytes(result));
    }

    /**
     * It takes a NetworkParams class from BitcoinJ specifying network parameters, and returns a ProtocolConfiguration
     * class, which also defines parmeters of the network but also adds more detail and some fine-grained
     * configurations, like specific configurations for controlling the Handshake, Ping/Pong, Discovery Algorithm, etc.
     */
    public static ProtocolConfig get(NetworkParameters params) {
        // We define each one the different Handlers Configurations:

        // Basic configuration:
        ProtocolBasicConfig basicConfig = ProtocolBasicConfig.builder()
                .minPeers(OptionalInt.of(MIN_PEERS_DEFAULT))
                .maxPeers(OptionalInt.of(MAX_PEERS_DEFAULT))
                .id(params.getId())
                .magicPackage(convertMagicPackageFronBitcoinJ(params.getPacketMagic()))
                .port(params.getPort())
                .build();

        // Messages Serialization Configuration:
        MessageHandlerConfig msgConfig = MessageHandlerConfig.builder()
                .basicConfig(basicConfig)
                .build();

        // Handshake Configuration:
        HandshakeHandlerConfig handshakeConfig = HandshakeHandlerConfig.builder()
                .basicConfig(basicConfig)
                .build();

        // Ping Pong Configuration:
        PingPongHandlerConfig pingPongConfig = PingPongHandlerConfig.builder()
                .basicConfig(basicConfig)
                .build();

        // Discovery Configuration:
        DiscoveryHandlerConfig discoveryConfig = DiscoveryHandlerConfig.builder()
                .basicConfig(basicConfig)
                .dns(params.getDnsSeeds())
                .build();

        // Blacklist Configuration:
        BlacklistHandlerConfig blacklistConfig = BlacklistHandlerConfig.builder()
                .basicConfig(basicConfig)
                .build();

        // Block Downloader Config:
        BlockDownloaderHandlerConfig downloaderConfig = BlockDownloaderHandlerConfig.builder()
                .basicConfig(basicConfig)
                .build();

        // We build the Block Header for the Genesis Block:
        // NOTE: This Genesis BlockHeader is needed to check in some cases that we are in the right Chain. Basically,
        // only the Hash is important. For other fields, we populate them as long as the values can be extracted from
        // the NetworkParams class in bitcoinJ. If they can NOT be extracted from it, we use dummy values
        // (the whole block definition used to be defined in JCL, but now we are leveraging on bitcoinJ for that, so we
        // only used the fields defined in bitcoinJ).

        BlockHeaderMsg genesisBlock = BlockHeaderMsg.builder()
                .creationTimestamp(params.genesisTime())
                .difficultyTarget(params.genesisDifficulty())
                .nonce(params.genesisNonce())
                .prevBlockHash(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.getBytes()).build())
                // Dummy Merkle Root
                .merkleRoot(HashMsg.builder().hash(Sha256Hash.wrap("0000000000000000000000000000000000000000000000000000000000000000").getBytes()).build())
                .build();

        // And we put everything together:
        ProtocolConfigImpl result = ProtocolConfigImpl.builder()
                .basicConfig(basicConfig)
                .messageConfig(msgConfig)
                .handshakeConfig(handshakeConfig)
                .discoveryConfig(discoveryConfig)
                .pingPongConfig(pingPongConfig)
                .blacklistConfig(blacklistConfig)
                .blockDownloaderConfig(downloaderConfig)
                .genesisBlock(genesisBlock)
                .build();

        return result;
    }

}
