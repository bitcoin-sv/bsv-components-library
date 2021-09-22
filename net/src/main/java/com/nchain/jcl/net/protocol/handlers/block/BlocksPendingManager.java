package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.network.PeerAddress;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
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
    public BlocksPendingManager onlyDownloadAfterAnnouncement() {
        this.onlyDownloadAfterAnnouncement = true;
        return this;
    }

    /** It enabled the flag so Blocks are downloaded from their announcers first, and from any other peers otherwise */
    public BlocksPendingManager downloadFromAnnouncersFirst() {
        this.downloadFromAnnouncersFirst = true;
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

    private Optional<PeerAddress> getAnnouncer(String blockHash) {
        return blockAnnouncements.entrySet().stream()
                .filter(entry -> entry.getValue().contains(blockHash))
                .map(entry -> entry.getKey())
                .findFirst();
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


    private boolean isThisBlockSuitableForThisPeer(String blockHash, PeerAddress peerAddress, List<PeerAddress> availablePeers) {

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
                if (anyPriorityPeerAvailable) {
                    return false;
                }
            }
        }

        // If the Peers can only be downloaded form those Peers who announced them, we check this Peer:
        // If the Peer has NOT been announced, then this block is SKIPPED

        if (this.onlyDownloadAfterAnnouncement) {
            result = isBlockAnnouncedBy(blockHash, peerAddress);
            return result;
        }

        // If the Peers that announce the blocks have Priority over others, then this block can be assigned to this
        // Peer IF:
        //  - it's been announced by this Peer, OR...
        //  - it's NOT been announced by any Peer, OR...
        //  - it's been announced by another Peer, but that Peer is NOT available anymore

        if (this.downloadFromAnnouncersFirst) {
            if (isBlockAnnouncedBy(blockHash, peerAddress)) {
                return true;
            } else {
                Optional<PeerAddress> announcerPeer = getAnnouncer(blockHash);
                if (announcerPeer.isEmpty() || (!availablePeers.contains(announcerPeer.get()))) {
                    return true;
                } else {
                    return false;
                }
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
