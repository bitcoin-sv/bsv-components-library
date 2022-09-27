package io.bitcoinsv.jcl.net.protocol.handlers.block;

import com.google.common.collect.ImmutableList;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 17/09/2021
 *
 * This class encapsulates the logic to store the pending blocks and also how to decide WHICH peer to choose to
 * download a particular block. This decision can be made based on multiple criteria, and multiple actions can
 * be performed depending on the situation (if no best suitable peer is found, for example).
 * In order to run all that logic, this class also stores additional info like blocks announcements, block downloads
 * attempts, etc.
 */
public class BlocksPendingManager {

    private static Logger log = LoggerFactory.getLogger(BlocksPendingManager.class);

    /**
     * A Download Request is a Request where a Peer/Block is given, asking if its possible to start the downloading of
     * that block from that specific Peer
     */
    public static class DownloadRequest {
        private PeerAddress peerAddress;
        private String blockHash;
        public DownloadRequest(PeerAddress peerAddress, String blockHash) {
            this.peerAddress = peerAddress;
            this.blockHash = blockHash;
        }
        public PeerAddress getPeerAddress() { return this.peerAddress;}
        public String getBlockHash()        { return this.blockHash;}
    }


    /**
     * A Download Response is the Response from a Previous Request. It represents the Block assigned to be downloaded
     * from a Peer, or some info about WHY that Peer cannot be assigned to any block at all.
     */

    // Note: A "announcer" Peer is a Peer that has "announced" a Block by sending previously an INV
    public enum DownloadResponseState {
        ASSIGNED,                       // Bock assigned to Peer (NORMAL CASE)
        TOO_MANY_FAILURES,              // Block NOT assigned: Peer has already tried this block before
        OTHER_PEER_WITH_EXCLUSIVITY,    // Block NOT assigned: Other Peer has higher priority for this Block
        OTHER_PEER_ANNOUNCER,           // Another Peer has announced this block so that Peer has higher Priority
        NO_ANNOUNCERS                   // The Block can ONLY be downloader from an Announcer, but this Peer is NOT
    }

    public static class DownloadResponse {
        private DownloadRequest request;
        private DownloadResponseState state;

        public DownloadResponse(DownloadRequest request) {
            this.request = request;
            this.state = DownloadResponseState.ASSIGNED;
        }

        public DownloadResponse(DownloadRequest request, DownloadResponseState state) {
            this.request = request;
            this.state = state;
        }

        public DownloadRequest getRequest()     { return this.request;}
        public DownloadResponseState getState() { return this.state;}
        public boolean isAssigned()             { return this.state.equals(DownloadResponseState.ASSIGNED);}
    }

    /**
     * This class stores the Response when we are asked about what Block can be assigned for the Peer given to
     * download. The response can be a single Peer if there is one, or a list of REJECTIONS, if no Block could
     * be assigned to this peer.
     */
    public static class DownloadFromPeerResponse {
        private PeerAddress peerAddress;
        private DownloadResponse assignedResponse;
        private List<DownloadResponse> rejectedResponses;

        public DownloadFromPeerResponse (DownloadResponse assignedResponse) {
            this.peerAddress = assignedResponse.getRequest().getPeerAddress();
            this.assignedResponse = assignedResponse;
        }

        public DownloadFromPeerResponse(PeerAddress peerAddress, List<DownloadResponse> rejectedResponses) {
            this.peerAddress = peerAddress;
            this.rejectedResponses = rejectedResponses;
        }

        public DownloadResponse getAssignedResponse()   { return this.assignedResponse;}
        public List<DownloadResponse> getRejections()   { return this.rejectedResponses;}
        public boolean isAssigned()                     { return assignedResponse != null;}

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            if (isAssigned()) {
                result.append(" Assigned ");
            } else {
                Map<DownloadResponseState, Long> rejectReasons = rejectedResponses.stream()
                        .collect(Collectors.groupingBy(DownloadResponse::getState, Collectors.counting()));
                result.append(rejectReasons.entrySet().stream()
                        .map(e -> e.getKey() + "(" + e.getValue() + ")")
                        .collect(Collectors.joining(" ")));
            }
            return result.toString();
        }
    }


    // BEST Match Logic:
    // A block can only be downloaded if there are some Peers connected. So if we have N Peers connected, then each
    // Peer is a POTENTIAL MATCH for a block to be downloaded from. Depending on Configuration, some Peers are a
    // better fit than others. So a BEST MATCH is a Peer which is the Best suitable Peer to download a block.

    // Indicates WHAT it is that defines a BEST Match:
    private BlockDownloaderHandlerConfig.BestMatchCriteria bestMatchCriteria = BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANYONE;

    // Indicates What to DO in case we have some BEST MATCH but are busy downloading other blocks:
    private BlockDownloaderHandlerConfig.BestMatchNotAvailableAction bestMatchNotAvailableAction = BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE;

    // Indicates what to do in case we have some Peers, but none of them is a BEST Match:
    private BlockDownloaderHandlerConfig.NoBestMatchAction noBestMatchAction = BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE;

    // List of pending blocks: It works as a FIFO Queue: First Block to be added are the first ones to be downloaded.
    private List<String> pendingBlocks = new ArrayList<>();
    private Set<String> pendingBlocksSet = new HashSet<>();

    // Blocks announced by Peer: [Key: peer. Value: List of Blocks announced by this peer]
    private Map<PeerAddress, Set<String>> blockAnnouncements = new ConcurrentHashMap<>();

    // Block Peers exclusivity: [Key: block Hash, Value: The ONLY Peers allowed to download this Block]
    private Map<String, PeerAddress> blocksPeerExclusivity = new ConcurrentHashMap<>();

    // Block Peers priority: [Key: block Hash, Value: in case of various options, these Peers will be selected first]
    private Map<String, Set<PeerAddress>> blocksPeerPriority = new ConcurrentHashMap<>();

    // Blocks download Attempts: (removed after successful download) [Key: Bock Hash, Value: Number of download Attempts]
    private Map<String, Integer> blocksNumDownloadAttempts = new ConcurrentHashMap<>();

    // Minimal time that should pass since last download failure of particular block from particular peer to be able for another try
    // TODO: this should be configurable
    private static final Duration minTimeSinceLastDownloadFailure = Duration.ofMinutes(3);

    // Register downloading failure of particular block on particular peer.
    // [Key: block hash, Value: Map [Key: peer address: Value time of the last download failure]]
    private Map<String, Map<PeerAddress, Instant>> downloadFails = new ConcurrentHashMap<>();

    // If "restrictedMode" is TRUE, then ONLY those pending blocks that have been tried already will be candidates for
    // a download. In this "restrictive Mode", no new Blocks are download, only "old" ones are re-tried, and for these
    // blocks the "BestMatch" Criteria and Actions do NOt apply: they are download from any peer available and as soon
    // as possible
    private boolean restrictedMode = false;

    /** Constructor */
    public BlocksPendingManager() { }

    // Best Match Policies/Criteria Setters:
    public void setBestMatchCriteria(BlockDownloaderHandlerConfig.BestMatchCriteria bestMatchCriteria) {
        this.bestMatchCriteria = bestMatchCriteria;
    }
    public void setNoBestMatchAction(BlockDownloaderHandlerConfig.NoBestMatchAction noBestMatchAction) {
        this.noBestMatchAction = noBestMatchAction;
    }
    public void setBestMatchNotAvailableAction(BlockDownloaderHandlerConfig.BestMatchNotAvailableAction bestMatchNotAvailableAction) {
        this.bestMatchNotAvailableAction = bestMatchNotAvailableAction;
    }

    // BOCK ANNOUNCEMENTS:
    public void registerBlockAnnouncement(String blockHash, PeerAddress peerAddress) {
        Set<String> blocks = blockAnnouncements.containsKey(peerAddress) ? blockAnnouncements.get(peerAddress) : new HashSet<>();
        blocks.add(blockHash);
        blockAnnouncements.put(peerAddress, blocks);
    }

    private boolean isBlockAnnouncedBy(String blockHash, PeerAddress peerAddress) {
        return blockAnnouncements.containsKey(peerAddress) && blockAnnouncements.get(peerAddress).contains(blockHash);
    }

    private boolean isBlockAnnouncedBy(String blockHash, List<PeerAddress> peerAddress) {
        return peerAddress.stream().anyMatch(p -> isBlockAnnouncedBy (blockHash, p));
    }

    // BLOCK EXCLUSIVITY/PRIORITY:
    public void registerBlockExclusivity(List<String> blockHashes, PeerAddress peerAddress) {
        blockHashes.forEach(blockHash -> blocksPeerExclusivity.put(blockHash, peerAddress));
    }

    public void registerBlockPriority(List<String> blockHashes, PeerAddress peerAddress) {
        blockHashes.forEach(blockHash -> {
            Set<PeerAddress> peers = blocksPeerPriority.get(blockHash);
            if (peers == null) {
                peers = new HashSet<>();
            }
            peers.add(peerAddress);
            blocksPeerPriority.put(blockHash, peers);
        });
    }

    // Register failed download attempt of a block from a peer
    public void registerDownloadFailure(String blockHash, PeerAddress peerAddress) {
        downloadFails.putIfAbsent(blockHash, new ConcurrentHashMap<>());
        downloadFails.get(blockHash).put(peerAddress, Instant.now());
    }

    // REGISTER OF EVENTS:
    public synchronized void registerNewDownloadAttempt(String blockHash)  { blocksNumDownloadAttempts.merge(blockHash, 1, (o, n) -> o + n); }
    public synchronized void registerBlockDownloaded(String blockHash)     { blocksNumDownloadAttempts.remove(blockHash);}
    public synchronized void registerBlockDiscarded(String blockHash)      { blocksNumDownloadAttempts.remove(blockHash);}
    public synchronized void registerBlockCancelled(String blockHash)      {
        blocksNumDownloadAttempts.remove(blockHash);
        pendingBlocks.remove(blockHash);
    }

    // RESTRICTED MODE:
    public synchronized void switchToRestrictedMode()                   { this.restrictedMode = true; }
    public synchronized void switchToNormalMode()                       { this.restrictedMode = false; }

    // DOWNLOAD ATTEMPTS:
    public int getNumDownloadAttempts(String blockHash)                 { return blocksNumDownloadAttempts.containsKey(blockHash)? blocksNumDownloadAttempts.get(blockHash) : 0; }
    public Map<String, Integer> getBlockDownloadAttempts()              { return blocksNumDownloadAttempts; }
    public boolean isBlockBeingAttempted(String blockHash)              { return blocksNumDownloadAttempts.containsKey(blockHash); }

    // PENDING BLOCKS:
    public synchronized void add(String blockHash)                      { add(List.of(blockHash)); }
    public synchronized void add(List<String> blockHashes)              {
        for (String hash : blockHashes) {
            if (pendingBlocksSet.add(hash)) {
                pendingBlocks.add(hash);
            }
        }
    }
    public synchronized void addWithPriority(String blockHash)          { addWithPriority(List.of(blockHash)); }
    public synchronized void addWithPriority(List<String> blockHashes)  {
        for (String hash : blockHashes) {
            if (pendingBlocksSet.add(hash)) {
                pendingBlocks.add(0, hash);
            }
        }
    }
    public synchronized void remove(String blockHash)                   { pendingBlocks.remove(blockHash); }
    public synchronized int size()                                      { return this.pendingBlocks.size(); }
    public synchronized List<String> getPendingBlocks()                 { return ImmutableList.copyOf(this.pendingBlocks); }
    public synchronized boolean contains(String blockHash)              { return this.pendingBlocks.contains(blockHash); }

    /**
     * This methods checks if a given Block can be assigned to the Peer given (currentPeer) to be download from it.
     * Depending on the CRITERIA and ACTIONS defined, some logic needs to be performed in order to pick up the right
     * Peer to download from. So the return of this method is:
     * - TRUE: The current Peer will be used to download the Block
     * - FALSE: The current Peer will NOT be used to download the Block. In this case, since this method will iterate
     *   other all the Peers, another Peer might be chosen for that (this method might return TRUE for other Peer)
     *
     * @param request           Download Assignment Request
     * @param availablePeers    List of available Pers (excluding 'currentPeer')
     * @param notAvailablePeers List of NOT available Pers (excluding 'currentPeer')
     * @return                  true -> This block can be assigned to this Peer for download
     */
    private DownloadResponse isPeerSuitableForDownload(
            DownloadRequest request,
            List<PeerAddress> availablePeers,
            List<PeerAddress> notAvailablePeers) {

        PeerAddress peer = request.getPeerAddress();
        String blockHash = request.getBlockHash();

        // By default, we assign this block to this Peer:
        DownloadResponse result = new DownloadResponse(request);

        // If "currentPeer" failed to download "blockHash" in last "minTimeSinceLastDownloadFailure" we will
        // return false.
        var failedAttemptsOnCurrentPeer = downloadFails.get(blockHash);
        if (failedAttemptsOnCurrentPeer != null) {
            var failedAttemptTimeForBlock = failedAttemptsOnCurrentPeer.get(peer);
            if (failedAttemptTimeForBlock != null) {
                // see if time since the last download attempt is less than "minTimeSinceLastDownloadFailure"
                if (Duration.between(failedAttemptTimeForBlock, Instant.now()).minus(minTimeSinceLastDownloadFailure).isNegative()) {
                    return new DownloadResponse(request, DownloadResponseState.TOO_MANY_FAILURES);
                }
            }
        }

        // If we are running in RestrictiveMode, we just assign this Block to this Peer and return:
        if (restrictedMode) return result;

        // If this Block has been assigned to one specific Peer to be downloaded from exclusively, we check if this
        // is that peer. If its not, then this Block is NOT assigned.

        if (this.blocksPeerExclusivity.containsKey(blockHash)) {
            boolean hasExclusivityForThisblock = this.blocksPeerExclusivity.get(blockHash).equals(peer);
            return hasExclusivityForThisblock
                    ? result
                    : new DownloadResponse(request, DownloadResponseState.OTHER_PEER_WITH_EXCLUSIVITY);
        }

        // If this block has been assigned a list of Peers to download from with priority, we check:
        // If this Peer is one of the assigned Peers, we assign it (return TRUE).
        // If its not:
        // - If this block has been assigned a list of Priority Peers and any of those Peers is available, then we just
        //   return FALSE, so we skip the process for this Peer so this block can be assigned to that peer with priority
        //   in another call to this method.
        // - If this block has NOT been assigned a list of Priority Peers, we just continue...

        if (this.blocksPeerPriority.containsKey(blockHash)) {
            if (!this.blocksPeerPriority.get(blockHash).contains(peer)) {
                boolean anyPriorityPeerAvailable = availablePeers.stream().anyMatch(p -> this.blocksPeerPriority.get(blockHash).contains(p));
                if (anyPriorityPeerAvailable) {
                    result = new DownloadResponse(request, DownloadResponseState.OTHER_PEER_WITH_EXCLUSIVITY);
                }
            }
            return result;
        }

        // If blocks can be downloaded from ANYONE, we return TRUE right away:
        if (this.bestMatchCriteria == BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANYONE) {
            return result;
        }

        // If Blocks can only be downloaded from those Peers who announced them:
        if (this.bestMatchCriteria == BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS) {
            if (isBlockAnnouncedBy(blockHash, request.getPeerAddress())) {
                // Announced by this Peer. We assign it
                return result;
            } else if (isBlockAnnouncedBy(blockHash, availablePeers)) {
                // Announced by OTHER available Peer. WE skip this one (Return false)
                return new DownloadResponse(request, DownloadResponseState.OTHER_PEER_ANNOUNCER);
            } else if (isBlockAnnouncedBy(blockHash, notAvailablePeers)) {
                // Block has been announced by a Peer that is NOT available. We do based on Action defined:
                if ((bestMatchNotAvailableAction == BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE)) {
                    // It can be Download by ANYBODY. We assign it:
                    return result;
                } else {
                    // It cannot be Assigned to this Peer:
                    return new DownloadResponse(request, DownloadResponseState.OTHER_PEER_ANNOUNCER);
                }

            } else {
                // Block has NOT been announced by ANY Peer at all. We do based on Action defined:
                if (noBestMatchAction == BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE) {
                    // It can be Download by ANYBODY. We assign it:
                    return result;
                } else {
                    // It cannot be Assigned to this Peer:
                    return new DownloadResponse(request, DownloadResponseState.NO_ANNOUNCERS);
                }
            }
        }

        return result;
    }

    /**
     * Given the currentPeer, it assigns a Block to download from it, from the list of pending Blocks. Since due to the
     * different CRITERIA or ACTION defined this election might be "complex", we also need extra info about what other
     * Peers we are currently connected to: available and NOT available.
     *
     * @param currentPeer           Peer we want to assign a Block to download
     * @param availablePeers        List of Peers we are connected to and available for download
     * @param notAvailablePeers     List of Peers we are connected bo but are NOT available (they are already busy
     *                              downloading other blocks).
     * @return  A block to assign to this Peer, or empty if no assignment is possible (because there are no pending
     *          blocks anymore, or because due to the CRITERIA and ACTIONS defined there is no match possible).
     */
    public synchronized Optional<DownloadFromPeerResponse> extractMostSuitableBlockForDownload(PeerAddress currentPeer,
                                                                             List<PeerAddress> availablePeers,
                                                                             List<PeerAddress> notAvailablePeers) {

        // Default:
        Optional<DownloadFromPeerResponse> result = Optional.empty();

        // If we are in NORMAL Mode, we loop over the "pending" list of Blocks checking for each one if this Peer is a
        // Best Match. If we are in RESTRICTIVE Mode, we loop instead over the list of ONLY those blocks that have been
        // tried already...

        List<String> blocksToProcess = (!restrictedMode)
                ? this.pendingBlocks
                : this.blocksNumDownloadAttempts.keySet().stream().filter(hash -> pendingBlocks.contains(hash)).collect(Collectors.toList());

        if (blocksToProcess.size() > 0) {
            // We loop over the Blocks, making a DownloadRequest for each one, and storing the response. If we
            // finally get a Match, then we only return that one, otherwise we return the list of rejections
            List<DownloadResponse> rejections = new ArrayList<>();

            for (int i = 0; i < blocksToProcess.size(); i++) {
                String blockHash = blocksToProcess.get(i);
                DownloadRequest request = new DownloadRequest(currentPeer, blockHash);
                DownloadResponse response = isPeerSuitableForDownload(request, availablePeers, notAvailablePeers);
                if (response.isAssigned()) {
                    // We found a MATCH:
                    // That block will then have to be REMOVED from the list of "pending" blocks.
                    //  - If we are in NORMAL Mode, we can remove it quickly by using its INDEX.
                    //  - If we are in RESTRICTIVE mode it might take longer...
                    if (!restrictedMode) { this.pendingBlocks.remove(i); }          // remove by Index
                    else                 { this.pendingBlocks.remove(blockHash);}   // remove by content

                    return Optional.of(new DownloadFromPeerResponse(response));
                } else {
                    // This block can NOT be Downloaded from this Peer. We save the Rejection:
                    rejections.add(response);
                }
            }
            // If we get to here, then no block could be assigned to this Peer:
            return Optional.of(new DownloadFromPeerResponse(currentPeer, rejections));

        }
        return result;
    }
}