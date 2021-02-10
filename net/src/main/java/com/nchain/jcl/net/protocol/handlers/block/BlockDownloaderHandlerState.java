package com.nchain.jcl.net.protocol.handlers.block;


import com.nchain.jcl.tools.handlers.HandlerState;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the state of the blockDownloader Handler at a point in time.
 */
public final class BlockDownloaderHandlerState extends HandlerState {

    private final List<String> pendingBlocks;
    private final List<String> downloadedBlocks;
    private final List<String> discardedBlocks;
    private final List<BlockPeerInfo.BlockProgressInfo> blocksProgress;

    public BlockDownloaderHandlerState(List<String> pendingBlocks, List<String> downloadedBlocks, List<String> discardedBlocks, List<BlockPeerInfo.BlockProgressInfo> blocksProgress) {
        this.pendingBlocks = pendingBlocks;
        this.downloadedBlocks = downloadedBlocks;
        this.discardedBlocks = discardedBlocks;
        this.blocksProgress = blocksProgress;
    }

    public static BlockDownloaderHandlerStateBuilder builder() {
        return new BlockDownloaderHandlerStateBuilder();
    }

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

    public List<String> getPendingBlocks()                              { return this.pendingBlocks; }
    public List<String> getDownloadedBlocks()                           { return this.downloadedBlocks; }
    public List<String> getDiscardedBlocks()                            { return this.discardedBlocks; }
    public List<BlockPeerInfo.BlockProgressInfo> getBlocksProgress()    { return this.blocksProgress; }

    public BlockDownloaderHandlerStateBuilder toBuilder() {
        return new BlockDownloaderHandlerStateBuilder().pendingBlocks(this.pendingBlocks).downloadedBlocks(this.downloadedBlocks).discardedBlocks(this.discardedBlocks).blocksProgress(this.blocksProgress);
    }

    /**
     * Builder
     */
    public static class BlockDownloaderHandlerStateBuilder {
        private List<String> pendingBlocks;
        private List<String> downloadedBlocks;
        private List<String> discardedBlocks;
        private List<BlockPeerInfo.BlockProgressInfo> blocksProgress;

        BlockDownloaderHandlerStateBuilder() {
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder pendingBlocks(List<String> pendingBlocks) {
            this.pendingBlocks = pendingBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder downloadedBlocks(List<String> downloadedBlocks) {
            this.downloadedBlocks = downloadedBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder discardedBlocks(List<String> discardedBlocks) {
            this.discardedBlocks = discardedBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksProgress(List<BlockPeerInfo.BlockProgressInfo> blocksProgress) {
            this.blocksProgress = blocksProgress;
            return this;
        }

        public BlockDownloaderHandlerState build() {
            return new BlockDownloaderHandlerState(pendingBlocks, downloadedBlocks, discardedBlocks, blocksProgress);
        }
    }
}
