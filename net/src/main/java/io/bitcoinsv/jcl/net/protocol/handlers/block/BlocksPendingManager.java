package io.bitcoinsv.jcl.net.protocol.handlers.block;

import com.google.common.collect.ImmutableList;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.handlers.block.strategies.BlockDownloadStrategy;
import io.bitcoinsv.jcl.net.protocol.handlers.block.strategies.DownloadRequest;
import io.bitcoinsv.jcl.net.protocol.handlers.block.strategies.DownloadResponse;
import io.bitcoinsv.jcl.net.protocol.handlers.block.strategies.PriorityStrategy;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
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

    // List of pending blocks: "ListOrderedSet" is a List with Set capabilities: It preserves the insertion order and
    // also avoid duplicates
    private ListOrderedSet<String> pendingBlocks = new ListOrderedSet<String>();

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

    // Strategy that decides whether a Block can be assigned to a Peer to download it from
    private PriorityStrategy downloadStrategy;

    /** Constructor */
    public BlocksPendingManager() { }

    // Best Match Policies/Criteria Setters:
    public void setDownloadStrategy(PriorityStrategy downloadStrategy) {
        this.downloadStrategy = downloadStrategy;
    }

    // Gets a reference to the Download Strrategy
    public PriorityStrategy getStrategy() {
        return this.downloadStrategy;
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
    public synchronized void add(List<String> blockHashes)              { pendingBlocks.addAll(blockHashes); }
    public synchronized void addWithPriority(String blockHash)          { addWithPriority(List.of(blockHash)); }
    public synchronized void addWithPriority(List<String> blockHashes)  { pendingBlocks.addAll(0, blockHashes);}
    public synchronized void remove(String blockHash)                   { pendingBlocks.remove(blockHash); }
    public synchronized int size()                                      { return this.pendingBlocks.size(); }
    public synchronized List<String> getPendingBlocks()                 { return ImmutableList.copyOf(this.pendingBlocks); }
    public synchronized boolean contains(String blockHash)              { return this.pendingBlocks.contains(blockHash); }

    public synchronized void registerBlockAsAlreadyAttempted(List<String> blockHashes) {
        blockHashes.forEach(blockHash -> blocksNumDownloadAttempts.merge(blockHash, 1, (o, n) -> o + n));
    }

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
    private Optional<DownloadResponse> isPeerSuitableForDownload(
            DownloadRequest request,
            List<PeerAddress> availablePeers,
            List<PeerAddress> notAvailablePeers) {

        PeerAddress peer = request.getPeerAddress();
        String blockHash = request.getBlockHash();

        log.trace("Downloading Block {} : Strategy [{}] : Checking if can be downloaded by {}...",
                request.getBlockHash(), downloadStrategy.getClass().getSimpleName(), request.getPeerAddress());

        // By default, we assign this block to this Peer:
        Optional<DownloadResponse> result = Optional.of(new DownloadResponse(request));

        // If this Peer failed to download "blockHash" in last "minTimeSinceLastDownloadFailure" we will
        // return false:
        var failedAttemptsOnCurrentPeer = downloadFails.get(blockHash);
        if (failedAttemptsOnCurrentPeer != null) {
            var failedAttemptTimeForBlock = failedAttemptsOnCurrentPeer.get(peer);
            if (failedAttemptTimeForBlock != null) {
                // see if time since the last download attempt is less than "minTimeSinceLastDownloadFailure"
                if (Duration.between(failedAttemptTimeForBlock, Instant.now()).minus(minTimeSinceLastDownloadFailure).isNegative()) {
                    log.trace("Downloading Block {} : Peer {} discarded (recently failed)", request.getBlockHash(), request.getPeerAddress());
                    return Optional.of(new DownloadResponse(request, DownloadResponse.DownloadResponseState.TOO_MANY_FAILURES));
                }
            }
        }

       // If we reach this peer, we leave the decision to the Download Strategy:
        result = downloadStrategy.requestDownload(request, availablePeers, notAvailablePeers);

        // If the result from the Strategy is a NOT MACH but we are running in RestrictiveMode, we just assign
        // this Block to this Peer and return:
        if ((result.isEmpty() || !result.get().isAssigned()) && restrictedMode) {
            log.debug("Downloading Block {} : Peer {} assigned (Restrictive Mode)", request.getBlockHash(), request.getPeerAddress());
            return Optional.of(new DownloadResponse(request));
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
    public synchronized Optional<DownloadResponse> extractMostSuitableBlockForDownload(PeerAddress currentPeer,
                                                                                       List<PeerAddress> availablePeers,
                                                                                       List<PeerAddress> notAvailablePeers) {

        // Default:
        Optional<DownloadResponse> result = Optional.empty();

        // If we are in NORMAL Mode, we loop over the "pending" list of Blocks checking for each one if this Peer is a
        // Best Match. If we are in RESTRICTIVE Mode, we loop instead over the list of ONLY those blocks that have been
        // tried already...

        ListOrderedSet<String> blocksToProcess = this.pendingBlocks;
        if (restrictedMode) {
            blocksToProcess = new ListOrderedSet<>();
            blocksToProcess.addAll(this.blocksNumDownloadAttempts.keySet().stream().filter(hash -> pendingBlocks.contains(hash)).collect(Collectors.toSet()));
        }

        if (!blocksToProcess.isEmpty()) {

            // We loop over the Blocks, making a DownloadRequest for each one, and storing the response.
            for (int i = 0; i < blocksToProcess.size(); i++) {
                // We try to assign this Block to the Current Peer:
                String blockHash = blocksToProcess.get(i);
                DownloadRequest request = new DownloadRequest(currentPeer, blockHash);
                Optional<DownloadResponse> response = isPeerSuitableForDownload(request, availablePeers, notAvailablePeers);
                if (response.isPresent()) {
                    if (response.get().isAssigned()) {
                        log.debug("Downloading Block {} : Assigned to Peer {}", blockHash, currentPeer);
                        // We found a MATCH:
                        // That block will then have to be REMOVED from the list of "pending" blocks.
                        //  - If we are in NORMAL Mode, we can remove it quickly by using its INDEX.
                        //  - If we are in RESTRICTIVE mode it might take longer...
                        if (!restrictedMode) { this.pendingBlocks.remove(i); }          // remove by Index
                        else                 { this.pendingBlocks.remove(blockHash);}   // remove by content
                        return response;
                    }
                }
                // We keep looking, another Block...
            } // for Blocks...
        }
        return result;
    }

}