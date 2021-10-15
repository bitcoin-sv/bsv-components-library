package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockDownloader Handler
 */

public class BlockDownloaderHandlerConfig extends HandlerConfig {

    // Default Values:
    public static final Duration DEFAULT_MAX_BLOCK_DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);
    public static final Duration DEFAULT_MAX_PEER_IDLE_TIMEOUT      = Duration.ofSeconds(30);
    public static final Duration DEFAULT_RETRY_WAITING_TIMEOUT      = Duration.ofMinutes(5);
    public static final Integer  DEFAULT_MAX_BLOCK_ATTEMPTS         = 5;
    public static final Integer  DEFAULT_MAX_DOWNLOADS_IN_PARALLEL  = 1;
    public static final Integer  DEFAULT_MAX_MB_IN_PARALLEL         = 100;
    public static final Duration DEFAULT_CLEANING_HISTORY_TIMEOUT   = Duration.ofMinutes(10);
    public static final Duration DEFAULT_INACTIVITY_TO_FAIL_TIMEOUT = Duration.ofSeconds(30);

    // Basic protocol Config:
    private ProtocolBasicConfig basicConfig;

    /** Max time to wait for a Block to be downloaded + Deserialized */
    private Duration maxDownloadTimeout = DEFAULT_MAX_BLOCK_DOWNLOAD_TIMEOUT;

    /** MAx time to wait for a Peer to send some bytes */
    private Duration maxIdleTimeout = DEFAULT_MAX_PEER_IDLE_TIMEOUT;

    /** Time to wait until we re-try to download a block after it's been discarded */
    private Duration retryDiscardedBlocksTimeout = DEFAULT_RETRY_WAITING_TIMEOUT;

    /** Maximum number of Attempts to download a Bock before finally giving up on it */
    private int maxDownloadAttempts = DEFAULT_MAX_BLOCK_ATTEMPTS;

    /** Maximum Blocks to download at the same time */
    private int maxBlocksInParallel = DEFAULT_MAX_DOWNLOADS_IN_PARALLEL;

    /** If true, the download history of each block will be erased after the block is successfully download */
    private boolean removeBlockHistoryAfterDownload = true;

    /** If specified, all blocks history will be removed after this duration has passed since their last activity */
    private Duration blockHistoryTimeout = DEFAULT_CLEANING_HISTORY_TIMEOUT;

    /** Maximum total Size of Blocks taht can be download in parallel */
    private long maxMBinParallel = DEFAULT_MAX_MB_IN_PARALLEL;

    /**
     * When a block download fails for whatever reason (idle peer, early disconnection, etc), the block is
     * moved to a LIMBO state, and it will remain there for some time before we definitely acknowledge that
     * there is areal problem (and not just multithread events coming in the wrong order or things like that).
     * The time a Block remains in LIMBO is determined by this variable.
     */
    private Duration inactivityTimeoutToFail = DEFAULT_INACTIVITY_TO_FAIL_TIMEOUT;

    // The Following ENUMS store different Criteria/Strategies to follow when Choosing the right Peer to download
    // a Block from or what to do if there is no clear match:
    // NOTE:
    // - We assume that we are connected to one Peer or more: Of these Peres, some might be already downloading blocks
    //   and others might be idle:
    //      - The Peers downloading are NOT AVAILABLE for Download at this moment
    //      - The idle Peers are AVAILABLE for Downloading.
    //
    // In order to download another Block, some Peers need to be AVAILABLE. So the CRITERIA and ACTIONS defined below
    // are only used when we need to download a new Block and we DO have some AVAILABLE Peers.
    //
    // From all the Available Peers, some are a BEST MATCH, which means that are a better fit than others, based on the
    // Criteria defined. Sometimes a BEST MATCH cannot be found, but we can STILL download the block from another Peer
    // that might not be as good as a BEST MATCH, but good enough.
    //
    // The Criteria and Actions to follow on each scenario are defined in the structures below:


    /** Different ways to choose the BEST MATCH of a Peer to download a Block from */
    public enum BestMatchCriteria {
        FROM_ANYONE,        // From first Peer available
        FROM_ANNOUNCERS     // From a Peer that has announce (INV) the Block
    }

    /**
     * In case a Match has been found BUT its NOT AVAILABLE because its downloading another Block,
     * here we specify what to do:
     */
    enum BestMatchNotAvailableAction {
        DOWNLOAD_FROM_ANYONE,   // We download it from first other available peer if possible
        WAIT                    // We do NOT download it yet, we wait instead for the Match to be available
    }

    /** In case there is no Best Match, here we specify what do to */
    enum NoBestMatchAction {
        DOWNLOAD_FROM_ANYONE, // We download it from first other available peer if possible
        WAIT                  // We do NOT download it yet, we wait instead for next Match
    }

    // Default values for BestMatch strategies:
    // - We download from ANY PEER (First available)
    // - Rest of configurations are NOT really needed

    private BestMatchCriteria           bestMatchCriteria = BestMatchCriteria.FROM_ANYONE;
    private BestMatchNotAvailableAction bestMatchNotAvailableAction = BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE;
    private NoBestMatchAction           noBestMatchAction = NoBestMatchAction.DOWNLOAD_FROM_ANYONE;

    public BlockDownloaderHandlerConfig(ProtocolBasicConfig basicConfig,
                                        Duration maxDownloadTimeout,
                                        Duration maxIdleTimeout,
                                        Duration retryDiscardedBlocksTimeout,
                                        Integer maxDownloadAttempts,
                                        Integer maxBlocksInParallel,
                                        boolean removeBlockHistoryAfterDownload,
                                        long maxMBinParallel,
                                        Duration blockHistoryTimeout,
                                        Duration inactivityTimeoutToFail,
                                        BestMatchCriteria bestMatchCriteria,
                                        BestMatchNotAvailableAction bestMatchNotAvailableAction,
                                        NoBestMatchAction noBestMatchAction) {
        this.basicConfig = basicConfig;
        if (maxDownloadTimeout != null)             this.maxDownloadTimeout = maxDownloadTimeout;
        if (maxIdleTimeout != null)                 this.maxIdleTimeout = maxIdleTimeout;
        if (retryDiscardedBlocksTimeout != null)    this.retryDiscardedBlocksTimeout = retryDiscardedBlocksTimeout;
        if (maxDownloadAttempts != null)            this.maxDownloadAttempts = maxDownloadAttempts;
        if (maxBlocksInParallel != null)            this.maxBlocksInParallel = maxBlocksInParallel;
        this.removeBlockHistoryAfterDownload = removeBlockHistoryAfterDownload;
        this.maxMBinParallel = maxMBinParallel;
        this.blockHistoryTimeout = blockHistoryTimeout;
        this.inactivityTimeoutToFail = inactivityTimeoutToFail;
        this.bestMatchCriteria = bestMatchCriteria;
        this.bestMatchNotAvailableAction = bestMatchNotAvailableAction;
        this.noBestMatchAction = noBestMatchAction;
    }

    public BlockDownloaderHandlerConfig() {}

    public ProtocolBasicConfig getBasicConfig()             { return this.basicConfig; }
    public Duration getMaxDownloadTimeout()                 { return this.maxDownloadTimeout; }
    public Duration getMaxIdleTimeout()                     { return this.maxIdleTimeout; }
    public Duration getRetryDiscardedBlocksTimeout()        { return this.retryDiscardedBlocksTimeout; }
    public int getMaxDownloadAttempts()                     { return this.maxDownloadAttempts; }
    public int getMaxBlocksInParallel()                     { return this.maxBlocksInParallel; }
    public boolean isRemoveBlockHistoryAfterDownload()      { return this.removeBlockHistoryAfterDownload; }
    public long getMaxMBinParallel()                        { return this.maxMBinParallel;}
    public Duration getBlockHistoryTimeout()                { return this.blockHistoryTimeout;}
    public Duration getInactivityTimeoutToFail()            { return this.inactivityTimeoutToFail;}

    public BestMatchCriteria getBestMatchCriteria()                     { return this.bestMatchCriteria;}
    public BestMatchNotAvailableAction getBestMatchNotAvailableAction() { return this.bestMatchNotAvailableAction;}
    public NoBestMatchAction getNoBestMatchAction()                     { return this.noBestMatchAction;}

    public BlockDownloaderHandlerConfigBuilder toBuilder() {
        return new BlockDownloaderHandlerConfigBuilder()
                .basicConfig(this.basicConfig)
                .maxDownloadTimeout(this.maxDownloadTimeout)
                .maxIdleTimeout(this.maxIdleTimeout)
                .retryDiscardedBlocksTimeout(this.retryDiscardedBlocksTimeout)
                .maxDownloadAttempts(this.maxDownloadAttempts)
                .maxBlocksInParallel(this.maxBlocksInParallel)
                .removeBlockHistoryAfterDownload(this.removeBlockHistoryAfterDownload)
                .maxMBinParallel(this.maxMBinParallel)
                .removeBlockHistoryAfter(this.blockHistoryTimeout)
                .inactivityTimeoutToFail(this.inactivityTimeoutToFail)
                .bestMatchCriteria(this.bestMatchCriteria)
                .bestMatchNotAvailableAction(this.bestMatchNotAvailableAction)
                .noBestMatchAction(this.noBestMatchAction);
    }

    public static BlockDownloaderHandlerConfigBuilder builder() {
        return new BlockDownloaderHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlockDownloaderHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;
        private Duration maxDownloadTimeout;
        private Duration maxIdleTimeout;
        private Duration retryDiscardedBlocksTimeout;
        private Integer maxDownloadAttempts;
        private Integer maxBlocksInParallel;
        private boolean removeBlockHistoryAfterDownload = true;
        private long maxMBinParallel = DEFAULT_MAX_MB_IN_PARALLEL;
        private Duration blockHistoryTimeout = DEFAULT_CLEANING_HISTORY_TIMEOUT;
        private Duration inactivityTimeoutToFail = DEFAULT_INACTIVITY_TO_FAIL_TIMEOUT;

        private BestMatchCriteria           bestMatchCriteria = BestMatchCriteria.FROM_ANYONE;
        private BestMatchNotAvailableAction bestMatchNotAvailableAction = BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE;
        private NoBestMatchAction           noBestMatchAction = NoBestMatchAction.DOWNLOAD_FROM_ANYONE;

        BlockDownloaderHandlerConfigBuilder() { }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder maxDownloadTimeout(Duration maxDownloadTimeout) {
            this.maxDownloadTimeout = maxDownloadTimeout;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder maxIdleTimeout(Duration maxIdleTimeout) {
            this.maxIdleTimeout = maxIdleTimeout;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder retryDiscardedBlocksTimeout(Duration retryDiscardedBlocksTimeout) {
            this.retryDiscardedBlocksTimeout = retryDiscardedBlocksTimeout;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder maxDownloadAttempts(int maxDownloadAttempts) {
            this.maxDownloadAttempts = maxDownloadAttempts;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder maxBlocksInParallel(int maxBlocksInParallel) {
            this.maxBlocksInParallel = maxBlocksInParallel;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder removeBlockHistoryAfterDownload(boolean removeBlockHistoryAfterDownload) {
            this.removeBlockHistoryAfterDownload = removeBlockHistoryAfterDownload;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder maxMBinParallel(long maxMBinParallel) {
            this.maxMBinParallel = maxMBinParallel;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder removeBlockHistoryAfter(Duration blockHistoryTimeout) {
            this.blockHistoryTimeout = blockHistoryTimeout;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder inactivityTimeoutToFail(Duration inactivityTimeoutToFail) {
            this.inactivityTimeoutToFail = inactivityTimeoutToFail;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder bestMatchCriteria(BestMatchCriteria bestMatchCriteria) {
            this.bestMatchCriteria = bestMatchCriteria;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder bestMatchNotAvailableAction(BestMatchNotAvailableAction bestMatchNotAvailableAction) {
            this.bestMatchNotAvailableAction = bestMatchNotAvailableAction;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder noBestMatchAction(NoBestMatchAction noBestMatchAction) {
            this.noBestMatchAction = noBestMatchAction;
            return this;
        }

        public BlockDownloaderHandlerConfig build() {
            return new BlockDownloaderHandlerConfig(
                    basicConfig,
                    maxDownloadTimeout,
                    maxIdleTimeout,
                    retryDiscardedBlocksTimeout,
                    maxDownloadAttempts,
                    maxBlocksInParallel,
                    removeBlockHistoryAfterDownload,
                    maxMBinParallel,
                    blockHistoryTimeout,
                    inactivityTimeoutToFail,
                    bestMatchCriteria,
                    bestMatchNotAvailableAction,
                    noBestMatchAction);
        }
    }
}
