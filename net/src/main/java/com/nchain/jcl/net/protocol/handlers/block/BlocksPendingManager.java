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


    public synchronized Optional<String> extractMostSuitableBlockForDownload(PeerAddress peerAddress, List<PeerAddress> availablePeers) {

        // Default:
        Optional<String> result = Optional.empty();

        // If there are no pending blocks, we don't assign anything...
        if (this.pendingBlocks.size() == 0) { return result; }

        // To return a suitable block to download, we loop over the pending pool and perform some verifications on
        // each one until we find the best one. So the difference between multiple scenarios is the verification itself
        // that can be expressed as a IntPredicate (taking the index of the Pool as a parameter):

        // Default: we return the next block in the Pool. the check will stop at the first element
        IntPredicate blockCheck = i -> true;

        // If 'onlyDownloadAfterAnnouncement == TRUE', we return a Block announced by this Peer:
        if (onlyDownloadAfterAnnouncement) {
            blockCheck = i -> isBlockAnnouncedBy(this.pendingBlocks.get(i), peerAddress);
        }

        // If 'downloadFromAnnouncersFirst == TRUE', we return a Block that:
        //  - it's been announced by this Peer, OR...
        //  - it's NOT been announced by any Peer, OR...
        //  - it's been announced by another Peer, but that Peer is NOT available anymore

        if (downloadFromAnnouncersFirst) {
            blockCheck = i -> {
                if (isBlockAnnouncedBy(this.pendingBlocks.get(i), peerAddress)) {
                    return true;
                } else {
                    Optional<PeerAddress> announcerPeer = getAnnouncer(this.pendingBlocks.get(i));
                    if (announcerPeer.isEmpty() || (announcerPeer.isPresent() && !availablePeers.contains(announcerPeer.get()))) {
                        return true;
                    } else { return false;}
                }
            };
        }

        // Now we just return the Block that meets the Predicate...
        OptionalInt blockIndexToReturn = IntStream.range(0, this.pendingBlocks.size())
                .filter(blockCheck)
                .findFirst();

        // We 'extract' the block from the pending List and return it:
        if (blockIndexToReturn.isPresent()) {
            result = Optional.of(this.pendingBlocks.get(blockIndexToReturn.getAsInt()));
            this.pendingBlocks.remove(blockIndexToReturn.getAsInt());
        }

        return result;
    }
}
