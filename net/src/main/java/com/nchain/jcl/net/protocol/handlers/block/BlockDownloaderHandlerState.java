package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.base.tools.handlers.HandlerState;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:20
 *
 * It stores the state of the blockDownloader Handler at a point in time.
 */
@Value
@Builder(toBuilder = true)
public class BlockDownloaderHandlerState extends HandlerState {
    private List<String> pendingBlocks;
    private List<String> downloadedBlocks;
    private List<String> discardedBlocks;
    private List<BlockPeerInfo.BlockProgressInfo> blocksProgress;

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Block Downloader State: ");
        result.append(downloadedBlocks.size() + " downloaded, ");
        result.append(discardedBlocks.size() + " discarded, ");
        result.append(pendingBlocks.size() + " pending, ");
        result.append(blocksProgress.size() + " in progress");
        result.append("\n");
        blocksProgress.forEach(b -> result.append(b.toString()).append("\n"));
        return result.toString();
    }
}
