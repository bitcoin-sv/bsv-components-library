package io.bitcoinsv.jcl.net.protocol.handlers.block;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.tools.handlers.HandlerState;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the state of the blockDownloader Handler at a point in time.
 */
public final class BlockDownloaderHandlerState extends HandlerState {

    // General Downloading Config:
    private BlockDownloaderHandlerConfig config;

    // Downloading State:
    private final BlockDownloaderHandlerImpl.DonwloadingState downloadingState;

    // List of blocks in different States:
    private final List<String> pendingBlocks;
    private final List<String> downloadedBlocks;
    private final List<String> discardedBlocks;
    private final List<String> pendingToCancelBlocks;
    private final List<String> cancelledBlocks;
    private final Set<String> blocksInLimbo;
    private Map<String, Instant> blocksLastActivity = new ConcurrentHashMap<>();

    // Blocks download History:
    private final Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory;

    // Complete info of all the Peers, but only those who are hanshaked at the moment (they might be idle or not)
    private final List<BlockPeerInfo> peersInfo;

    // Number of total download re-attemps since the beginning:
    private final long totalReattempts;

    // Map with the current attempts per block:
    private Map<String, Integer> blocksNumDownloadAttempts;

    // percentage of "busy": If 100, then the Block downloader is downloading all the possible blocks simultaneously,
    // based on configuration. If 50, only half of the blocks that could be are being downloaded, etc.
    // This parameter is NOT a snapshot, but more like an accumulative aggregation: Its the average between the
    // current value and the previous one...
    private final int busyPercentage;

    // If FALSE, then the download process is not allowed at the moment in order to prevent bandwith:
    private final boolean bandwidthRestricted;

    private final long blocksDownloadingSize;

    public BlockDownloaderHandlerState( BlockDownloaderHandlerConfig config,
                                        BlockDownloaderHandlerImpl.DonwloadingState downloadingState,
                                        List<String> pendingBlocks,
                                        List<String> downloadedBlocks,
                                        List<String> discardedBlocks,
                                        List<String> pendingToCancelBlocks,
                                        List<String> cancelledBlocks,
                                        Set<String> blocksInLimbo,
                                        Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory,
                                        Map<String, Instant> blocksLastActivity,
                                        List<BlockPeerInfo> peersInfo,
                                        long totalReattempts,
                                        Map<String, Integer> blocksNumDownloadAttempts,
                                        int busyPercentage,
                                        boolean bandwidthRestricted,
                                        long blocksDownloadingSize) {
        this.config = config;
        this.downloadingState = downloadingState;
        this.pendingBlocks = pendingBlocks;
        this.downloadedBlocks = downloadedBlocks;
        this.discardedBlocks = discardedBlocks;
        this.pendingToCancelBlocks = pendingToCancelBlocks;
        this.cancelledBlocks = cancelledBlocks;
        this.blocksInLimbo = blocksInLimbo;
        this.blocksHistory = blocksHistory;
        this.blocksLastActivity = blocksLastActivity;
        this.peersInfo = peersInfo;
        this.totalReattempts = totalReattempts;
        this.blocksNumDownloadAttempts = blocksNumDownloadAttempts;
        this.busyPercentage = busyPercentage;
        this.bandwidthRestricted = bandwidthRestricted;
        this.blocksDownloadingSize = blocksDownloadingSize;
    }

    public static BlockDownloaderHandlerStateBuilder builder() {
        return new BlockDownloaderHandlerStateBuilder();
    }

    public String toStringShort() {
        StringBuffer result = new StringBuffer();

        long totalSizeDownloadingInMB = getBlocksDownloadingSize() / 1_000_000;
        result.append("\nStatus: ").append(downloadingState);
        result.append(", downloading ").append(totalSizeDownloadingInMB).append("MBs ");
        if (bandwidthRestricted) {
            result.append("(bandwidth restricted)");
        }

        result.append("\nBlocks: ");
        result.append(downloadedBlocks.size() + " downloaded, " + getNumPeersDownloading() + " downloading, " + pendingBlocks.size() + " pending, ");
        result.append(blocksInLimbo.size() + " in limbo, ");
        result.append(discardedBlocks.size() + " discarded, ");
        result.append(cancelledBlocks.size() + " canceled, "+ pendingToCancelBlocks.size() + " pending to cancel, ");
        result.append(totalReattempts + " re-attempts");

        result.append("\nPeers: ");
        // Peers connected but discarded
        int numPeersDiscarded = (int) peersInfo.stream()
                .filter(p -> p.isHandshaked())
                .filter(p -> p.isDiscarded())
                .count();
        // Peers processing blocks:
        int numPeersProcessing = (int) peersInfo.stream()
                .filter(p -> p.isHandshaked())
                .filter(p -> p.isProcessing())
                .count();
        // Peers Idle:
        int numPeersIdle = (int) peersInfo.stream()
                .filter(p -> p.isHandshaked())
                .filter(p -> p.isIdle())
                .count();
        result.append(peersInfo.size()).append(" Handshaked, ");
        result.append(numPeersDiscarded).append(" discarded, ");
        result.append(numPeersProcessing).append(" downloading ");
        result.append("(").append(busyPercentage).append( "% busy ").append("), ");
        result.append(numPeersIdle).append(" idle");
        return result.toString();
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("\nBLOCK-DOWNLOAD:");
        result.append("\n-------------------------------------------------------------------------------------");

        // Block Downloading Summary:
        result.append(toStringShort());

        // Details of Download in Progress:
        StringBuffer downloadInProgressStr = new StringBuffer();
        result.append("\nDownload in Process:");
        peersInfo.stream()
                .filter(p -> p.isProcessing())
                .forEach(p -> downloadInProgressStr.append("\n > ").append(p));
        result.append((downloadInProgressStr.length() > 0) ? downloadInProgressStr.toString(): " None");

        // Details of Blocks in Limbo:
        StringBuffer blocksInLimboStr = new StringBuffer();
        result.append("\nBlocks in Limbo:");
        blocksInLimbo.stream().forEach(blockHash -> {
            Duration timePassedSinceLastActivity = Duration.between(blocksLastActivity.get(blockHash), Instant.now());
            Duration timeRemainingInLimbo = config.getInactivityTimeoutToFail().minus(timePassedSinceLastActivity);
            blocksInLimboStr.append("\n > ").append(blockHash).append(" will retry in ").append(timeRemainingInLimbo.toSeconds()).append(" secs...");
        });
        result.append((blocksInLimboStr.length() > 0) ? blocksInLimboStr.toString(): " None");
        result.append("\n-------------------------------------------------------------------------------------");
        return result.toString();
    }

    public BlockDownloaderHandlerImpl.DonwloadingState getDownloadingState()
    { return this.downloadingState; }
    public List<String> getPendingBlocks()          { return this.pendingBlocks; }
    public List<String> getDownloadedBlocks()       { return this.downloadedBlocks; }
    public List<String> getDiscardedBlocks()        { return this.discardedBlocks; }
    public List<String> getPendingToCancelBlocks()  { return this.pendingToCancelBlocks; }
    public List<String> getCancelledBlocks()        { return cancelledBlocks; }
    public Set<String>  getBlocksInLimbo()          { return blocksInLimbo; }

    public List<BlockPeerInfo> getPeersInfo()   { return this.peersInfo; }
    public long getTotalReattempts()            { return this.totalReattempts;}
    public int getBusyPercentage()              { return this.busyPercentage;}
    public boolean isRunning()                  { return this.downloadingState.equals(BlockDownloaderHandlerImpl.DonwloadingState.RUNNING);}
    public boolean isPaused()                   { return this.downloadingState.equals(BlockDownloaderHandlerImpl.DonwloadingState.PAUSED);}
    public Map<String, Integer> getBlocksNumDownloadAttempts()
    { return this.blocksNumDownloadAttempts; }
    public Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> getBlocksHistory()
    { return this.blocksHistory;}

    public boolean isBandwidthRestricted()      { return bandwidthRestricted; }
    public long getBlocksDownloadingSize()      { return blocksDownloadingSize;}

    public long getNumPeersDownloading() {
        if (peersInfo == null) return 0;
        return peersInfo.stream()
                .filter( p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                .count();
    }

    public Optional<BlockPeerInfo> getPeerInfo(String blockHash) {
        if (this.peersInfo == null ) { return Optional.empty();}

        return this.peersInfo.stream()
                .filter(p -> p.getCurrentBlockInfo() != null)
                .filter(p -> p.getCurrentBlockInfo().getHash().equals(blockHash))
                .findFirst();
    }

    public BlockDownloaderHandlerStateBuilder toBuilder() {
        return new BlockDownloaderHandlerStateBuilder()
                .pendingBlocks(this.pendingBlocks)
                .downloadedBlocks(this.downloadedBlocks)
                .discardedBlocks(this.discardedBlocks)
                .pendingToCancelBlocks(this.pendingToCancelBlocks)
                .cancelledBlocks(this.cancelledBlocks)
                .blocksInLimbo(this.blocksInLimbo)
                .blocksHistory(this.blocksHistory)
                .peersInfo(this.peersInfo)
                .blocksNumDownloadAttempts(this.blocksNumDownloadAttempts)
                .busyPercentage(this.busyPercentage)
                .bandwidthRestricted(this.bandwidthRestricted)
                .blocksDownloadingSize(this.blocksDownloadingSize);
    }

    /**
     * Builder
     */
    public static class BlockDownloaderHandlerStateBuilder {
        private BlockDownloaderHandlerConfig config;
        private BlockDownloaderHandlerImpl.DonwloadingState downloadingState;
        private List<String> pendingBlocks;
        private List<String> downloadedBlocks;
        private List<String> discardedBlocks;
        private List<String> pendingToCancelBlocks;
        private List<String> cancelledBlocks;
        private Set<String> blocksInLimbo;
        private Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory;
        private Map<String, Instant> blocksLastActivity;
        private List<BlockPeerInfo> peersInfo;
        private long totalReattempts;
        private Map<String, Integer> blocksNumDownloadAttempts;
        private int busyPercentage;
        private boolean bandwidthRestricted;
        private long blocksDownloadingSize;

        BlockDownloaderHandlerStateBuilder() {
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder config(BlockDownloaderHandlerConfig config) {
            this.config = config;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder downloadingState(BlockDownloaderHandlerImpl.DonwloadingState downloadingState) {
            this.downloadingState = downloadingState;
            return this;
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

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder pendingToCancelBlocks(List<String> pendingToCancelBlocks) {
            this.pendingToCancelBlocks = pendingToCancelBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder cancelledBlocks(List<String> cancelledBlocks) {
            this.cancelledBlocks = cancelledBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksInLimbo(Set<String> blocksInLimbo) {
            this.blocksInLimbo = blocksInLimbo;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksHistory(Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory) {
            this.blocksHistory = blocksHistory;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksLastActivity(Map<String, Instant> blocksLastActivity) {
            this.blocksLastActivity = blocksLastActivity;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder peersInfo(List<BlockPeerInfo> peersInfo) {
            this.peersInfo = peersInfo;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder totalReattempts(long totalReattempts) {
            this.totalReattempts = totalReattempts;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksNumDownloadAttempts(Map<String, Integer> blocksNumDownloadAttempts) {
            this.blocksNumDownloadAttempts = blocksNumDownloadAttempts;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder busyPercentage(int busyPercentage) {
            this.busyPercentage = busyPercentage;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder bandwidthRestricted(boolean bandwidthRestricted) {
            this.bandwidthRestricted = bandwidthRestricted;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksDownloadingSize(long blocksDownloadingSize) {
            this.blocksDownloadingSize = blocksDownloadingSize;
            return this;
        }

        public BlockDownloaderHandlerState build() {
            return new BlockDownloaderHandlerState(config, downloadingState, pendingBlocks, downloadedBlocks, discardedBlocks, pendingToCancelBlocks, cancelledBlocks, blocksInLimbo, blocksHistory, blocksLastActivity, peersInfo, totalReattempts, blocksNumDownloadAttempts, busyPercentage, bandwidthRestricted, blocksDownloadingSize);
        }
    }
}
