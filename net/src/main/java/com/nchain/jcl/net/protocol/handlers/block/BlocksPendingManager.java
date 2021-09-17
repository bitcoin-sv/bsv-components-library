package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.net.network.PeerAddress;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    public synchronized Optional<String> extractMostSuitableBlockForDownload(PeerAddress peerAddress) {

        Optional<String> result = Optional.empty();

        // We search the Block that meets the criteria:
        // By default its the first block in the list If 'onlyDownloadBlocksAfterAnnouncement' is TRUE, then we
        // search for the first block in the pending pool announced by this Peer:

        if (this.pendingBlocks.size() > 0) {
            OptionalInt blockIndexToReturn = OptionalInt.of(0);
            if (onlyDownloadAfterAnnouncement) {
                blockIndexToReturn = IntStream.range(0, this.pendingBlocks.size())
                        .filter(i -> isBlockAnnouncedBy(this.pendingBlocks.get(i), peerAddress))
                        .findFirst();
            }

            if (blockIndexToReturn.isPresent()) {
                result = Optional.of(this.pendingBlocks.get(blockIndexToReturn.getAsInt()));
                this.pendingBlocks.remove(blockIndexToReturn.getAsInt());
            }
        }

        return result;
    }
}
