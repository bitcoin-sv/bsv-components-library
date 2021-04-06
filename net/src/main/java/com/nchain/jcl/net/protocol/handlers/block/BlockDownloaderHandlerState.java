package com.nchain.jcl.net.protocol.handlers.block;


import com.nchain.jcl.tools.handlers.HandlerState;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.Lock;

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
    private final List<BlockPeerInfo> peersInfo;


    public BlockDownloaderHandlerState(List<String> pendingBlocks,
                                       List<String> downloadedBlocks,
                                       List<String> discardedBlocks,
                                       List<BlockPeerInfo> peersInfo) {
        this.pendingBlocks = pendingBlocks;
        this.downloadedBlocks = downloadedBlocks;
        this.discardedBlocks = discardedBlocks;
        this.peersInfo = peersInfo;
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
        result.append(peersInfo.size() + " peers, ");

        long numPeersWorking = peersInfo.stream()
                .filter( p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                .count();

        result.append(numPeersWorking + " peers downloading Blocks, ");
        result.append("\n");

        // We print this Peer download Speed:
        DecimalFormat speedFormat = new DecimalFormat("#0.0");
        peersInfo.stream()
                .filter(p -> p.getWorkingState() == BlockPeerInfo.PeerWorkingState.PROCESSING)
                .forEach(p -> {
                    // we print basic status info:
                    result.append(p.getCurrentBlockInfo().toString());

                    // We print the download Speed info:
                    Integer peerSpeed = p.getDownloadSpeed();
                    String speedStr = (peerSpeed == null || p.getCurrentBlockInfo().bytesDownloaded == null)
                            ? "¿?"
                            : speedFormat.format((double) peerSpeed / 1_000);

                    result.append(" [ " + speedStr + " KB/sec ]");
                    // We print this Peer time info:
                    Duration lastActivityTimestamp = Duration.between(p.getCurrentBlockInfo().lastBytesReceivedTimestamp, Instant.now());
                    result.append(" [" + lastActivityTimestamp.toMillis() + " millisecs last read]");

                    result.append("\n");
                });



        return result.toString();
    }

    public List<String> getPendingBlocks()                              { return this.pendingBlocks; }
    public List<String> getDownloadedBlocks()                           { return this.downloadedBlocks; }
    public List<String> getDiscardedBlocks()                            { return this.discardedBlocks; }
    public List<BlockPeerInfo> getPeersInfo()                           { return this.peersInfo; }

    public BlockDownloaderHandlerStateBuilder toBuilder() {
        return new BlockDownloaderHandlerStateBuilder().pendingBlocks(this.pendingBlocks).downloadedBlocks(this.downloadedBlocks).discardedBlocks(this.discardedBlocks).peersInfo(this.peersInfo);
    }

    /**
     * Builder
     */
    public static class BlockDownloaderHandlerStateBuilder {
        private List<String> pendingBlocks;
        private List<String> downloadedBlocks;
        private List<String> discardedBlocks;
        private List<BlockPeerInfo> peersInfo;

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

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder peersInfo(List<BlockPeerInfo> peersInfo) {
            this.peersInfo = peersInfo;
            return this;
        }

        public BlockDownloaderHandlerState build() {
            return new BlockDownloaderHandlerState(pendingBlocks, downloadedBlocks, discardedBlocks, peersInfo);
        }
    }
}
