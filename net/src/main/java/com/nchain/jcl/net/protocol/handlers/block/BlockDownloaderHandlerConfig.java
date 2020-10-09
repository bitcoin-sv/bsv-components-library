package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
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

    // Basic protocol Config:
    private ProtocolBasicConfig basicConfig;

    /** Max time to wait for a Block to be downloaded + Deserialized */
    @Builder.Default
    private Duration maxDownloadTimeout = Duration.ofMinutes(10);

    /** MAx time to wait for a block to send some bytes */
    @Builder.Default
    private Duration maxIdleTimeout = Duration.ofSeconds(30);

    /** Time to wait until we re-try to download a block after it's been discarded */
    @Builder.Default
    private Duration retryDiscardedBlocksTimeout = Duration.ofMinutes(5);

    /** Maximum number of Attempts to download a Bock before finally giving up on it */
    @Builder.Default
    private int maxDownloadAttempts = 5;

    /** Maximum Blocks to download at the same time */
    @Builder.Default
    private int maxBlocksInParallel = 1;

}
