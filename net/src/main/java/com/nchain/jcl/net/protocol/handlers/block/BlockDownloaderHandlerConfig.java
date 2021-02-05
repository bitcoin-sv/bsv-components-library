package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockDownloader Handler
 */

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private Duration maxDownloadTimeout = DEFAULT_MAX_BLOCK_DOWNLOAD_TIMEOUT;

    /** MAx time to wait for a Peer to send some bytes */
    @Builder.Default
    private Duration maxIdleTimeout = DEFAULT_MAX_PEER_IDLE_TIMEOUT;

    /** Time to wait until we re-try to download a block after it's been discarded */
    @Builder.Default
    private Duration retryDiscardedBlocksTimeout = DEFAULT_RETRY_WAITING_TIMEOUT;

    /** Maximum number of Attempts to download a Bock before finally giving up on it */
    @Builder.Default
    private int maxDownloadAttempts = DEFAULT_MAX_BLOCK_ATTEMPTS;

    /** Maximum Blocks to download at the same time */
    @Builder.Default
    private int maxBlocksInParallel = DEFAULT_MAX_DOWNLOADS_IN_PARALLEL;

}
