package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.network.PeerAddress;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
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

    // This flag controls one of the criteria for downloading blocks:
    // If TRUE, then blocks are ONLY downloaded from those Peers that have announced them. If a Block is pending but
    // has NOT been announced by any peer, its NOT downloaded.
    private boolean onlyDownloadAfterAnnouncement;

    // This flag controls one of the criteria for downloading blocks:
    // If TRUE, then blocks are assigned to those Peers that have announced them first. but if they are not being
    // announced explicitly then they are assigned by other peer.
    private boolean downloadFromAnnouncersFirst;

    // List of pending blocks: It works as a FIFO Queue: First Block to be added are the first ones to be downloaded.
    // If 'onlyDownloadBlocksAfterAnnouncement' is TRUE, then this behaviour might be a bit different, since some Blocks
    // might skip validation in that case...
    private List<String> pendingBlocks = new ArrayList<>();

    // We register the Blocks announced by each Peer:
    private Map<PeerAddress, Set<String>> blockAnnouncements = new ConcurrentHashMap<>();

    // It stores the blocks to download exclusivity from a Peer
    private Map<String, PeerAddress> blocksPeerExclusivity = new ConcurrentHashMap<>();

    // It stores the Blocks to download from a specific Peer first, if possible:
    private Map<String, Set<PeerAddress>> blocksPeerPriority = new ConcurrentHashMap<>();

    /** Constructor */
    public BlocksPendingManager() {
    }

    /** It enables the Flag so the Blocks are ONLY downloaded from those Peers that have announced them */
    public BlocksPendingManager onlyDownloadAfterAnnouncement(boolean value) {
        this.onlyDownloadAfterAnnouncement = value;
        return this;
    }

    /** It enabled the flag so Blocks are downloaded from their announcers first, and from any other peers otherwise */
    public BlocksPendingManager downloadFromAnnouncersFirst(boolean value) {
        this.downloadFromAnnouncersFirst = value;
        return this;
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

    public synchronized void add(String blockHash) {
        this.pendingBlocks.add(blockHash);
    }

    public synchronized void addWithPriority(String blockHash) {
        this.pendingBlocks.add(0, blockHash);
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
     * This methods checks if a given Block can be assigned to the Peer given to be download from it.
     * Depending on the download priority configured, a Block might be better assigned to some Peer rather than others,
     * depending on whether the block has been announced by them or not, etc. So for that reason, in order to make the
     * decision we need to know not only the Block and Peer given, but also the list of other Peers that are also
     * available for Download.
     *
     * For example:
     * - We have 3 Peers: P1, P2, P3
     * - We have a pending list of 2 Blocks: B1, B2
     *
     * A loop is performed over the Peers, and for each peer, another loop is performed over the Blocks until a godd
     * fit is found:
     *  - isThisBlockSuitableForThisPeer(P1, B1, [P2,P3])
     *  - isThisBlockSuitableForThisPeer(P1, B2, [P2,P3])
     *  - isThisBlockSuitableForThisPeer(P2, B1, [P1,P3])
     *  - isThisBlockSuitableForThisPeer(P2, B2, [P1,P3])
     *  - isThisBlockSuitableForThisPeer(P3, B1, [P1,P2])
     *  - isThisBlockSuitableForThisPeer(P3, B2, [P1,P2])
     *
     * @param blockHash         Block Hash we want to download
     * @param peerAddress       Peer we try to check if its a good fit to download this block
     * @param availablePeers    List of other Peers also available for download (EXCLUDING 'peerAddress')
     * @return                  true -> This block can be assigned to this Peer for download
     */
    private boolean isThisBlockSuitableForThisPeer(String blockHash, PeerAddress peerAddress, List<PeerAddress> availablePeers) {

        // By default, we assign this block to this Peer:
        boolean result = true;


        // If this Block has been assigned to one specific Peer to be downloaded from exclusively, we check if this
        // is that peer. If its not, then this Block is NOT assigned.

        if (this.blocksPeerExclusivity.containsKey(blockHash)) {
            result = this.blocksPeerExclusivity.get(blockHash).equals(peerAddress);
            return result;
        }

        // If this block has been assigned a list of Peers to download from with priority, we heck if this Peer is
        // one of them. If it is, we assign it.
        // If its not:
        // - If this block has been assigned a list of Priority Peers and any of those Peers is available, then we just
        //   return FALSE, so we skip the process for this Peer so this block can be assigned to that peer with priority
        //    in another call to this method.
        // - If this block has NOT been assigned a list of Priority Peers, we just continue...

        if (this.blocksPeerPriority.containsKey(blockHash)) {
            if (this.blocksPeerPriority.get(blockHash).contains(peerAddress)) {
                return true;
            } else {
                boolean anyPriorityPeerAvailable = availablePeers.stream().anyMatch(p -> this.blocksPeerPriority.get(blockHash).contains(p));
                return (availablePeers.isEmpty() || !anyPriorityPeerAvailable);
            }
        }

        // If the Peers can only be downloaded from those Peers who announced them, we check this Peer:
        // If the Peer has NOT been announced, then this block is SKIPPED

        if (this.onlyDownloadAfterAnnouncement) {
            result = isBlockAnnouncedBy(blockHash, peerAddress);
            return result;
        }

        // If the Peers that announce the blocks have Priority over others, then this block can be assigned to this
        // Peer IF:
        //  - it's been announced by this Peer, OR...
        //  - it's NOT been announced by any Peer and it's not been announced by any available Peer either

        if (this.downloadFromAnnouncersFirst) {
            if (isBlockAnnouncedBy(blockHash, peerAddress)) {
                // The Block has been announced by this Peer, so we assign it :
                return true;
            } else {
                // The block has NOT been announced by this Peer.
                boolean isAnnnouncedByAnyAvailablePeer = availablePeers.stream().anyMatch(p -> isBlockAnnouncedBy(blockHash, p));
                return (availablePeers.isEmpty() || !isAnnnouncedByAnyAvailablePeer);
            }
        }

        return result;
    }

    public synchronized Optional<String> extractMostSuitableBlockForDownload(PeerAddress peerAddress, List<PeerAddress> availablePeers) {

        // Default:
        Optional<String> result = Optional.empty();

        if (this.pendingBlocks.size() > 0) {
            // Now we just return the Block that meets the check...
            OptionalInt blockIndexToReturn = IntStream.range(0, this.pendingBlocks.size())
                    .filter(i -> isThisBlockSuitableForThisPeer(this.pendingBlocks.get(i), peerAddress, availablePeers))
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
