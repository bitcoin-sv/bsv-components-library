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
    public static final Duration DEFAULT_CLEANING_HISTORY_TIMEOUT  = Duration.ofMinutes(10);

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

    /** If TRUE, Blocks are downloaded ONLY from those Peers that announced them */
    private boolean onlyDownloadAfterAnnouncement = false;

    /**
     * If TRUE, block are downloaded from Peers that have announced them, but if there are no such peers they are
     * downloaded by any other Peer
     */
    private boolean downloadFromAnnouncersFirst = false;

    public BlockDownloaderHandlerConfig(ProtocolBasicConfig basicConfig,
                                        Duration maxDownloadTimeout,
                                        Duration maxIdleTimeout,
                                        Duration retryDiscardedBlocksTimeout,
                                        Integer maxDownloadAttempts,
                                        Integer maxBlocksInParallel,
                                        boolean removeBlockHistoryAfterDownload,
                                        long maxMBinParallel,
                                        Duration blockHistoryTimeout,
                                        boolean onlyDownloadAfterAnnouncement,
                                        boolean downloadFromAnnouncersFirst) {
        this.basicConfig = basicConfig;
        if (maxDownloadTimeout != null)             this.maxDownloadTimeout = maxDownloadTimeout;
        if (maxIdleTimeout != null)                 this.maxIdleTimeout = maxIdleTimeout;
        if (retryDiscardedBlocksTimeout != null)    this.retryDiscardedBlocksTimeout = retryDiscardedBlocksTimeout;
        if (maxDownloadAttempts != null)            this.maxDownloadAttempts = maxDownloadAttempts;
        if (maxBlocksInParallel != null)            this.maxBlocksInParallel = maxBlocksInParallel;
        this.removeBlockHistoryAfterDownload = removeBlockHistoryAfterDownload;
        this.maxMBinParallel = maxMBinParallel;
        this.blockHistoryTimeout = blockHistoryTimeout;
        this.onlyDownloadAfterAnnouncement = onlyDownloadAfterAnnouncement;
        this.downloadFromAnnouncersFirst = downloadFromAnnouncersFirst;
    }

    public BlockDownloaderHandlerConfig() {}

    public ProtocolBasicConfig getBasicConfig()         { return this.basicConfig; }
    public Duration getMaxDownloadTimeout()             { return this.maxDownloadTimeout; }
    public Duration getMaxIdleTimeout()                 { return this.maxIdleTimeout; }
    public Duration getRetryDiscardedBlocksTimeout()    { return this.retryDiscardedBlocksTimeout; }
    public int getMaxDownloadAttempts()                 { return this.maxDownloadAttempts; }
    public int getMaxBlocksInParallel()                 { return this.maxBlocksInParallel; }
    public boolean isRemoveBlockHistoryAfterDownload()  { return this.removeBlockHistoryAfterDownload; }
    public long getMaxMBinParallel()                    { return this.maxMBinParallel;}
    public Duration getBlockHistoryTimeout()            { return this.blockHistoryTimeout;}
    public boolean isOnlyDownloadAfterAnnouncement()    { return this.onlyDownloadAfterAnnouncement;}
    public boolean isDownloadFromAnnouncersFirst()      { return this.downloadFromAnnouncersFirst;}

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
                .onlyDownloadAfterAnnouncement(this.onlyDownloadAfterAnnouncement)
                .downloadFromAnnouncersFirst(this.downloadFromAnnouncersFirst);
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
        private boolean onlyDownloadAfterAnnouncement = false;
        private boolean downloadFromAnnouncersFirst = false;

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

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder onlyDownloadAfterAnnouncement(boolean onlyDownloadAfterAnnouncement) {
            this.onlyDownloadAfterAnnouncement = onlyDownloadAfterAnnouncement;
            return this;
        }

        public BlockDownloaderHandlerConfig.BlockDownloaderHandlerConfigBuilder downloadFromAnnouncersFirst(boolean downloadFromAnnouncersFirst) {
            this.downloadFromAnnouncersFirst = downloadFromAnnouncersFirst;
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
                    onlyDownloadAfterAnnouncement,
                    downloadFromAnnouncersFirst);
        }
    }
}
