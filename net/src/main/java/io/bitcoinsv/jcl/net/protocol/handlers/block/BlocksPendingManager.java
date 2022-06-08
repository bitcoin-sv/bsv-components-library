package io.bitcoinsv.jcl.net.protocol.handlers.block;

import com.google.common.collect.ImmutableList;
import io.bitcoinsv.jcl.net.network.PeerAddress;

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

    // Blocks announced by Peer: [Key: peer. Value: List of Blocks announced by this peer]
    private Map<PeerAddress, Set<String>> blockAnnouncements = new ConcurrentHashMap<>();

    // Block Peers exclusivity: [Key: block Hash, Value: The ONLY Peers allowed to download this Block]
    private Map<String, PeerAddress> blocksPeerExclusivity = new ConcurrentHashMap<>();

    // Block Peers priority: [Key: block Hash, Value: in case of various options, these Peers will be selected first]
    private Map<String, Set<PeerAddress>> blocksPeerPriority = new ConcurrentHashMap<>();

    // Blocks download Attempts: (removed after successful download) [Key: Bock Hash, Value: Number of download Attempts]
    private Map<String, Integer> blocksNumDownloadAttempts = new ConcurrentHashMap<>();

    // If "restrictedMode" is TRUE, then ONLY those pending blocks that have been tried already will be candidates for
    // a download. In this "restrictive Mode", no new Blocks are download, only "old" ones are re-tried, and for these
    // blocks the "BestMatch" Criteria and Actions do NOt apply: they are download from any peer available and as soon
    // as possible
    private boolean restrictedMode = false;

    /** Constructor */
    public BlocksPendingManager() { }

    // Best Match Policies/Criteria Setters:
    public void setBestMatchCriteria(BlockDownloaderHandlerConfig.BestMatchCriteria bestMatchCriteria)   { this.bestMatchCriteria = bestMatchCriteria; }
    public void setNoBestMatchAction(BlockDownloaderHandlerConfig.NoBestMatchAction noBestMatchAction)   { this.noBestMatchAction = noBestMatchAction; }
    public void setBestMatchNotAvailableAction(BlockDownloaderHandlerConfig.BestMatchNotAvailableAction bestMatchNotAvailableAction)
                                                                            { this.bestMatchNotAvailableAction = bestMatchNotAvailableAction; }
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

    // REGISTER OF EVENTS:
    public void registerNewDownloadAttempt(String blockHash)            { blocksNumDownloadAttempts.merge(blockHash, 1, (o, n) -> o + n); }
    public void registerBlockDownloaded(String blockHash)               { blocksNumDownloadAttempts.remove(blockHash); }
    public void registerBlockDiscarded(String blockHash)                { blocksNumDownloadAttempts.remove(blockHash); }
    public void registerBlockCancelled(String blockHash)                {
        blocksNumDownloadAttempts.remove(blockHash);
        pendingBlocks.remove(blockHash);
    }

    // RESTRICTED MODE:
    public void switchToRestrictedMode()                                { this.restrictedMode = true; }
    public void switchToNormalMode()                                    { this.restrictedMode = false; }

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
    public synchronized List<String> getPendingBlocks()                 { return ImmutableList.copyOf(this.pendingBlocks); }
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

        // If we are running in RestrictiveMode, we just assign this Block to this Peer and return:
        if (restrictedMode) return true;

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
        if (this.bestMatchCriteria == BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANYONE) {
            return true;
        }

        // If Blocks can only be downloaded from those Peers who announced them:
        if (this.bestMatchCriteria == BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS) {
            if (isBlockAnnouncedBy(blockHash, currentPeer)) {
                result = true; // Announced by this Peer. We assign it
            } else if (isBlockAnnouncedBy(blockHash, availablePeers)){
                result = false; // Announced by OTHER available Peer. WE skip this one (Return false)
            } else if (isBlockAnnouncedBy(blockHash, notAvailablePeers)) {
                // Block has been announced by a Peer that is NOT available. We do based on Action defined:
                result = (bestMatchNotAvailableAction == BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE);

            } else {
                // Block has NOT been announced by ANY Peer at all. We do based on Action defined:
                result = (noBestMatchAction == BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE);
            }
            return result;
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
    public synchronized Optional<String> extractMostSuitableBlockForDownload(PeerAddress currentPeer,
                                                                             List<PeerAddress> availablePeers,
                                                                             List<PeerAddress> notAvailablePeers) {

        // Default:
        Optional<String> result = Optional.empty();

        // If we are in NORMAL Mode, we loop over the "pending" list of Blocks checking for each one if this Peer is a
        // Best Match. If we are in RESTRICTIVE Mode, we loop instead over the list of ONLY those blocks that have been
        // tried already...

        List<String> blocksToProcess = (!restrictedMode)
                ? this.pendingBlocks
                : this.blocksNumDownloadAttempts.keySet().stream().filter(hash -> pendingBlocks.contains(hash)).collect(Collectors.toList());

        if (blocksToProcess.size() > 0) {
            // We loop over the blocks and return the first one that is suitable (we get its index in the list):
            OptionalInt blockIndexToReturn = IntStream.range(0, blocksToProcess.size())
                    .filter(i -> isPeerSuitableForDownload(blocksToProcess.get(i), currentPeer, availablePeers, notAvailablePeers))
                    .findFirst();

            if (blockIndexToReturn.isPresent()) {
                result = Optional.of(blocksToProcess.get(blockIndexToReturn.getAsInt()));

                // That block will then have to be REMOVED from the list of "pending" blocks.
                //  - If we are in NORMAL Mode, we can remove it quickly by using its INDEX.
                //  - If we are in RESTRICTIVE mode it might take longer...

                if (!restrictedMode) {
                    this.pendingBlocks.remove(blockIndexToReturn.getAsInt()); // remove by Index -> FAST
                } else {
                    this.pendingBlocks.remove(result.get()); // remove by Content -> SLOW
                }
            }
        }
        return result;
    }
}