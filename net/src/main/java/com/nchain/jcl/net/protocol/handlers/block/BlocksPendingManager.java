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
 * This class encapsulate the pending blocks, and the logic that might be enabled so blocks are assigned for
 * download following some criteria.
 */
public class BlocksPendingManager {

    // Criteria and Action to perform to chose the BEST MATCH when downloading a Block:
    private BestMatchCriteria           bestMatchCriteria = BestMatchCriteria.FROM_ANYONE;
    private BestMatchNotAvailableAction bestMatchNotAvailableAction = BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE;
    private NoBestMatchAction           noBestMatchAction = NoBestMatchAction.DOWNLOAD_FROM_ANYONE;

    // List of pending blocks: It works as a FIFO Queue: First Block to be added are the first ones to be downloaded.
    // If 'onlyDownloadBlocksAfterAnnouncement' is TRUE, then this behaviour might be a bit different, since some Blocks
    // might skip validation in that case...
    private List<String> pendingBlocks = new ArrayList<>();

    // We register the Blocks announced by each Peer:
    private Map<PeerAddress, Set<String>> blockAnnouncements = new ConcurrentHashMap<>();

    // It stores the blocks to download exclusivity from a Peer
    private Map<String, PeerAddress> blocksPeerExclusivity = new ConcurrentHashMap<>();

    // It stores the Blocks to download from a specific Peers first, if possible:
    private Map<String, Set<PeerAddress>> blocksPeerPriority = new ConcurrentHashMap<>();

    /** Constructor */
    public BlocksPendingManager() {
    }

    public void setBestMatchCriteria(BestMatchCriteria bestMatchCriteria) {
        this.bestMatchCriteria = bestMatchCriteria;
    }

    public void setBestMatchNotAvailableAction(BestMatchNotAvailableAction bestMatchNotAvailableAction) {
        this.bestMatchNotAvailableAction = bestMatchNotAvailableAction;
    }

    public void setNoBestMatchAction(NoBestMatchAction noBestMatchAction) {
        this.noBestMatchAction = noBestMatchAction;
    }

    public void registerBlockAnnouncement(String blockHash, PeerAddress peerAddress) {
        Set<String> blocks = blockAnnouncements.get(peerAddress);
        if (blocks == null) {
            blocks = new HashSet<>();
        }
        blocks.add(blockHash);
        blockAnnouncements.put(peerAddress, blocks);
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

    private boolean isBlockAnnouncedBy(String blockHash, PeerAddress peerAddress) {
        return (blockAnnouncements.containsKey(peerAddress)) ? blockAnnouncements.get(peerAddress).contains(blockHash): false;
    }

    private boolean isBlockAnnouncedBy(String blockHash, List<PeerAddress> peerAddress) {
        return peerAddress.stream()
                .anyMatch(p -> (blockAnnouncements.containsKey(p) && blockAnnouncements.get(p).contains(blockHash)));
    }

    public synchronized void add(String blockHash) {
        this.pendingBlocks.add(blockHash);
    }

    public synchronized void add(List<String> blockHashes) {
        this.pendingBlocks.addAll(blockHashes);
    }

    public synchronized void addWithPriority(String blockHash) {
        this.pendingBlocks.add(0, blockHash);
    }

    public synchronized void addWithPriority(List<String> blockHashes) {
        this.pendingBlocks.addAll(0, blockHashes);
    }

    public synchronized void remove(String blockHash) {
        this.pendingBlocks.remove(blockHash);
    }

    public synchronized int size() {
        return this.pendingBlocks.size();
    }

    public List<String> getPendingBlocks() {
        return Collections.unmodifiableList(this.pendingBlocks);
    }

    public boolean contains(String blockHash) {
        return this.pendingBlocks.contains(blockHash);
    }


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

    public synchronized Optional<String> extractMostSuitableBlockForDownload(PeerAddress peerAddress,
                                                                             List<PeerAddress> availablePeers,
                                                                             List<PeerAddress> notAvailablePeers) {

        // Default:
        Optional<String> result = Optional.empty();

        if (this.pendingBlocks.size() > 0) {
            // Now we just return the Block that meets the check...
            OptionalInt blockIndexToReturn = IntStream.range(0, this.pendingBlocks.size())
                    .filter(i -> isPeerSuitableForDownload(this.pendingBlocks.get(i), peerAddress, availablePeers, notAvailablePeers))
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
