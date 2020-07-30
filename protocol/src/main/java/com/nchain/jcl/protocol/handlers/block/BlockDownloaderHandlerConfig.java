package com.nchain.jcl.protocol.handlers.block;

import com.nchain.jcl.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:37
 *
 * Configuration class for the BlockDownloader Handler
 */

// TODO: IMPROVE DOC of the methods in this Class
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
