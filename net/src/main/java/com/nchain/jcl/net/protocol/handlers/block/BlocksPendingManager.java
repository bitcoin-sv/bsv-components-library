package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 17/09/2021
 *
 * This class encapsulate the pending blocks, and the logic to follow to decide WHICH peer downloads from WHICH Block.
 */
public class BlocksPendingManager {

    // BEST Match Logic:
    // A block can only be downloaded if there are some Peers connected. So if we have N Peers connected, then each
    // Peer is a POTENTIAL MATCH for a block to be downloaded from. Depending on Configuration, some Peers are a
    // better fit tha others. So a BEST MATCH is a Peer which is the Best suitable Peer to download a block.
    //
    // So the Logic to decide WHICH Peer to choose is made for EVERY BLOCK.

    // Indicates WHAT it is that defines a BEST Match:
    private BestMatchCriteria bestMatchCriteria = BestMatchCriteria.FROM_ANYONE;

    // Indicates What to DO in case we have some Peers and also some of them are actually a BEST MATCH, BUT those
    // BEST MATCH Peers are not available at the moment (they are downloading other blocks):
    private BestMatchNotAvailableAction bestMatchNotAvailableAction = BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE;

    // Indicates what to do in case we have some Peers, but none of them is a BEST Match:
    private NoBestMatchAction noBestMatchAction = NoBestMatchAction.DOWNLOAD_FROM_ANYONE;

    // List of pending blocks: It works as a FIFO Queue: First Block to be added are the first ones to be downloaded.
    private List<String> pendingBlocks = new ArrayList<>();

    // Blocks announced by Peer:
    // Key: peer. Value: List of Blocks announced by this peer
    private Map<PeerAddress, Set<String>> blockAnnouncements = new ConcurrentHashMap<>();

    // Block Peers exclusivity:
    // Key: block Hash, Value: The only Peers allowed to download this Block
    private Map<String, PeerAddress> blocksPeerExclusivity = new ConcurrentHashMap<>();

    // Block Peers priority:
    // Key: block Hash, Value: in case of various options, these Peers will be selected first
    private Map<String, Set<PeerAddress>> blocksPeerPriority = new ConcurrentHashMap<>();

    // Blocks download Attempts: (record removed after successful download)
    // Key: Bock Hash, Value: Number of download Attempts
    private Map<String, Integer> blocksNumDownloadAttempts = new ConcurrentHashMap<>();

    // If TRUE, then ONLY those blocks that are still being downloaded (not finished yet) are available for download.
    // If FALSE, al pendings blocks are available (normal performance)
    // In a regular situation, it will be false (all blocks available), but in some scenarios the Download Process
    // can be paused, in that situation we still need to download those blocks that have been attempted but not finished
    // yet.
    private boolean onlyCurrentAttemptedBlocksAllowed = false;

    /** Constructor */
    public BlocksPendingManager() { }

    public void setBestMatchCriteria(BestMatchCriteria bestMatchCriteria) { this.bestMatchCriteria = bestMatchCriteria; }
    public void setNoBestMatchAction(NoBestMatchAction noBestMatchAction) { this.noBestMatchAction = noBestMatchAction; }
    public void setBestMatchNotAvailableAction(BestMatchNotAvailableAction bestMatchNotAvailableAction) {
        this.bestMatchNotAvailableAction = bestMatchNotAvailableAction;
    }


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

    public void registerNewDownloadAttempt(String blockHash)            { blocksNumDownloadAttempts.merge(blockHash, 1, (o, n) -> o + n); }
    public void registerBlockDownloaded(String blockHash)               { blocksNumDownloadAttempts.remove(blockHash); }
    public void registerBlockDiscarded(String blockHash)                { blocksNumDownloadAttempts.remove(blockHash); }
    public void registerBlockCancelled(String blockHash)                {
        blocksNumDownloadAttempts.remove(blockHash);
        pendingBlocks.remove(blockHash);
    }

    public void onlyCurrentAttemptedBlocksAllowed()                     { this.onlyCurrentAttemptedBlocksAllowed = true; }
    public void allBlocksAllowed()                                      { this.onlyCurrentAttemptedBlocksAllowed = false; }

    // DOWNLOAD ATTEMPTS:
    public int getNumDownloadAttempts(String blockHash)                 { return blocksNumDownloadAttempts.containsKey(blockHash)? blocksNumDownloadAttempts.get(blockHash) : 0; }
    public Map<String, Integer> getBlockDownloadAttempts()              { return blocksNumDownloadAttempts; }
    public boolean isBlockBeingAttempted(String blockHash)              { return blocksNumDownloadAttempts.containsKey(blockHash); }

    // PENDING BLOCKS:
    public synchronized void add(String blockHash)                      { this.pendingBlocks.add(blockHash); }
    public synchronized void add(List<String> blockHashes)              { this.pendingBlocks.addAll(blockHashes); }
    public synchronized void addWithPriority(String blockHash)          { this.pendingBlocks.add(0, blockHash); }
    public synchronized void addWithPriority(List<String> blockHashes)  { this.pendingBlocks.addAll(0, blockHashes); }
    public synchronized void remove(String blockHash)                   { this.pendingBlocks.remove(blockHash); }
    public synchronized int size()                                      { return this.pendingBlocks.size(); }
    public List<String> getPendingBlocks()                              { return Collections.unmodifiableList(this.pendingBlocks); }
    public boolean contains(String blockHash)                           { return this.pendingBlocks.contains(blockHash); }

    /**
     * This methods checks if a given Block can be assigned to the Peer given (currentPeer) to be download from it.
     * Depending on the CRITERIA and ACTIONS defined, some logic needs to be performed in order to pick up the right
     * Peer to download from. So the return of this method is:
     * - TRUE: The current Peer will be used to download the Block
     * - FALSE: The current Peer will NOT be used to download the Block. In this case, since this method will iterate
     *   other all the Peers, another Peer might be chosen for that (this method might return TRUE for other Peer)
     *
     * @param blockHash         Block Hash we want to download
     * @param currentPeer       Peer we try to check if its a good fit to download this block
     * @param availablePeers    List of available Pers (excluding 'currentPeer')
     * @param notAvailablePeers List of NOT available Pers (excluding 'currentPeer')
     * @return                  true -> This block can be assigned to this Peer for download
     */
    private boolean isPeerSuitableForDownload(String blockHash, PeerAddress currentPeer,
                                              List<PeerAddress> availablePeers,
                                              List<PeerAddress> notAvailablePeers) {

        // By default, we assign this block to this Peer:
        boolean result = true;


        // If this Block has been assigned to one specific Peer to be downloaded from exclusively, we check if this
        // is that peer. If its not, then this Block is NOT assigned.

        if (this.blocksPeerExclusivity.containsKey(blockHash)) {
            result = this.blocksPeerExclusivity.get(blockHash).equals(currentPeer);
            return result;
        }

        // If this block has been assigned a list of Peers to download from with priority, we check:
        // If this Peer is one of the assigned Peers, we assign it (return TRUE).
        // If its not:
        // - If this block has been assigned a list of Priority Peers and any of those Peers is available, then we just
        //   return FALSE, so we skip the process for this Peer so this block can be assigned to that peer with priority
        //   in another call to this method.
        // - If this block has NOT been assigned a list of Priority Peers, we just continue...

        if (this.blocksPeerPriority.containsKey(blockHash)) {
            if (this.blocksPeerPriority.get(blockHash).contains(currentPeer)) {
                result = true;
            } else {
                boolean anyPriorityPeerAvailable = availablePeers.stream().anyMatch(p -> this.blocksPeerPriority.get(blockHash).contains(p));
                if (anyPriorityPeerAvailable) {
                    result = false;
                }
            }
            return result;
        }

        // If blocks can be downloaded from ANYONE, we return TRUE right away:
        if (this.bestMatchCriteria == BestMatchCriteria.FROM_ANYONE) {
            return true;
        }

        // If Blocks can only be downloaded from those Peers who announced them:
        if (this.bestMatchCriteria == BestMatchCriteria.FROM_ANNOUNCERS) {
            if (isBlockAnnouncedBy(blockHash, currentPeer)) {
                result = true; // Announced by this Peer. We assign it
            } else if (isBlockAnnouncedBy(blockHash, availablePeers)){
                result = false; // Announced by OTHER available Peer. WE skip this one (Return false)
            } else if (isBlockAnnouncedBy(blockHash, notAvailablePeers)) {
                // Block has been announced by a Peer that is NOT available. We do based on Action defined:
                result = (bestMatchNotAvailableAction == BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE);

            } else {
                // Block has NOT been announced by ANY Peer at all. We do based on Action defined:
                result = (noBestMatchAction == NoBestMatchAction.DOWNLOAD_FROM_ANYONE);
            }
            return result;
        }

        return result;
    }

    /**
     * Given the currentPeer, it assigns Block to download from it, from the list of pending Blocks. Since due to the
     * different CRITERIA or ACTION defined this election might be "complex", we also need exta info about what other
     * Peers we are currently connected to: available and NOT available.
     *
     * @param currentPeer           Peer we want to assign a Block to download
     * @param availablePeers        List of Peers we are connected to and available for download
     * @param notAvailablePeers     List of Peers we are connected bo but are NOT available (they are already busy
     *                              downloading other blocks).
     * @return  A block to assign to this PEer, or empty if no assignment is possible (because there are no pending
     *          blocks anymore, or because due to the CRITERIA and ACTIONS defined there is no match possible.
     */
    public synchronized Optional<String> extractMostSuitableBlockForDownload(PeerAddress currentPeer,
                                                                             List<PeerAddress> availablePeers,
                                                                             List<PeerAddress> notAvailablePeers) {

        // Default:
        Optional<String> result = Optional.empty();

        if (this.pendingBlocks.size() > 0) {
            // Now we just return the Block that meets the check...
            OptionalInt blockIndexToReturn = IntStream.range(0, this.pendingBlocks.size())
                    .filter(i -> isPeerSuitableForDownload(this.pendingBlocks.get(i), currentPeer, availablePeers, notAvailablePeers))
                    .findFirst();

            // We 'extract' the block from the pending List and return it:
            if (blockIndexToReturn.isPresent()) {
                result = Optional.of(this.pendingBlocks.get(blockIndexToReturn.getAsInt()));
                this.pendingBlocks.remove(blockIndexToReturn.getAsInt());
            }
        }
        return result;
    }
}