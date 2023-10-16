package io.bitcoinsv.jcl.integration;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.params.Net;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2023 nChain Ltd
 */
public class ChainDownloadConfig {

    private static Logger log = LoggerFactory.getLogger(ChainDownloadConfig.class);

    // Default Config values:
    public static final Integer DEF_MIN_PEERS = 10;
    public static final Integer DEF_MAX_PEERS = 20;

    // Configuration to run Block-Download:
    private Net net;
    private int minPeers;
    private int maxPeers;
    private Sha256Hash initialBlock;

    public ChainDownloadConfig(String ...args) throws IllegalArgumentException {
        try {
            log.info("Loading Command-Line Arguments:");
            // Net to connect to:
            net = Net.valueOf(args[0].toUpperCase());
            // Initial Block to start downloading from:
            initialBlock = Sha256Hash.wrap(args[1]);
            // Min and Max Peers:
            minPeers = (args.length > 2)? Integer.parseInt(args[2]) : DEF_MIN_PEERS;
            maxPeers = (args.length > 3)? Integer.parseInt(args[3]) : DEF_MAX_PEERS;
            log.info(" - Network: {}", net);
            log.info(" - Initial Block: {}", initialBlock);
            log.info(" - minPeers: {}", minPeers);
            log.info(" - maxPeers: {}", maxPeers);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Net getNet()                 { return net;}
    public int getMinPeers()            { return minPeers;}
    public int getMaxPeers()            { return maxPeers;}
    public Sha256Hash getInitialBlock() { return initialBlock;}
}
