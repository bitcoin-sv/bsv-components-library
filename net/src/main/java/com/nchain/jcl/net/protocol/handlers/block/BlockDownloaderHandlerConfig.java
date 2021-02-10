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

    public BlockDownloaderHandlerConfig(ProtocolBasicConfig basicConfig, Duration maxDownloadTimeout, Duration maxIdleTimeout, Duration retryDiscardedBlocksTimeout, Integer maxDownloadAttempts, Integer maxBlocksInParallel) {
        this.basicConfig = basicConfig;
        if (maxDownloadTimeout != null)             this.maxDownloadTimeout = maxDownloadTimeout;
        if (maxIdleTimeout != null)                 this.maxIdleTimeout = maxIdleTimeout;
        if (retryDiscardedBlocksTimeout != null)    this.retryDiscardedBlocksTimeout = retryDiscardedBlocksTimeout;
        if (maxDownloadAttempts != null)            this.maxDownloadAttempts = maxDownloadAttempts;
        if (maxBlocksInParallel != null)            this.maxBlocksInParallel = maxBlocksInParallel;
    }

    public BlockDownloaderHandlerConfig() {}

    public ProtocolBasicConfig getBasicConfig()         { return this.basicConfig; }
    public Duration getMaxDownloadTimeout()             { return this.maxDownloadTimeout; }
    public Duration getMaxIdleTimeout()                 { return this.maxIdleTimeout; }
    public Duration getRetryDiscardedBlocksTimeout()    { return this.retryDiscardedBlocksTimeout; }
    public int getMaxDownloadAttempts()                 { return this.maxDownloadAttempts; }
    public int getMaxBlocksInParallel()                 { return this.maxBlocksInParallel; }

    public BlockDownloaderHandlerConfigBuilder toBuilder() {
        return new BlockDownloaderHandlerConfigBuilder().basicConfig(this.basicConfig).maxDownloadTimeout(this.maxDownloadTimeout).maxIdleTimeout(this.maxIdleTimeout).retryDiscardedBlocksTimeout(this.retryDiscardedBlocksTimeout).maxDownloadAttempts(this.maxDownloadAttempts).maxBlocksInParallel(this.maxBlocksInParallel);
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

        public BlockDownloaderHandlerConfig build() {
            return new BlockDownloaderHandlerConfig(basicConfig, maxDownloadTimeout, maxIdleTimeout, retryDiscardedBlocksTimeout, maxDownloadAttempts, maxBlocksInParallel);
        }
    }
}
