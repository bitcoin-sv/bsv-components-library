package io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;

import io.bitcoinsv.bsvcl.common.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A priority strategy is the basic Strategy and its common to all Strategies, meaning the logic here is ALWAYS
 * taken into consideration when downloading blocks.
 *
 * This Strategy assigns a Peer to download a Block If that Peer has been assigned a HIGH priority or EXCLUSIVITY
 * over that Block. The terms "Priority" or "exclusivity" are specified by fields in the BlocksDownloadRequest
 * Event itself.
 *
 * This is not a Strategy that is used directly, but its is usually extended. This oe provides the Priority logic
 * which is common for all Strategies.
 */
public class PriorityStrategy extends BaseStrategy implements BlockDownloadStrategy {

    private static Logger log = LoggerFactory.getLogger(PriorityStrategy.class);

    // Block Peers exclusivity:
    // If a Peer has Exclusivity on a block, then that block can ONLY be download from that Peer
    // [Key: block Hash, Value: The ONLY Peers allowed to download this Block]
    private Map<String, PeerAddress> blocksPeerExclusivity = new ConcurrentHashMap<>();

    // Block Peers priority:
    // If some Peers have priority over a Block, they will be chose before an other consideration
    // [Key: block Hash, Value:
    private Map<String, Set<PeerAddress>> blocksPeerPriority = new ConcurrentHashMap<>();

    /** Constructor */
    public PriorityStrategy(BlockDownloaderHandlerConfig.BestMatchCriteria bestMatchCriteria, EventBus eventBus) {
        super(bestMatchCriteria, eventBus);
    }

    /** It registers a Block Exclusivity */
    public void registerBlockExclusivity(List<String> blockHashes, PeerAddress peerAddress) {
        blockHashes.forEach(blockHash -> blocksPeerExclusivity.put(blockHash, peerAddress));
    }

    /** It Registers a block Priority */
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

    @Override
    public Optional<DownloadResponse> requestDownload(
            DownloadRequest request,
            List<PeerAddress> availablePeers,
            List<PeerAddress> notAvailablePeers) {
        PeerAddress peer = request.getPeerAddress();
        String blockHash = request.getBlockHash();

        // Default: No Assignment
        Optional<DownloadResponse> result = Optional.empty();

        // We Check EXCLUSIVITY:
        // If this Block has been assigned to one specific Peer to be downloaded from exclusively, we check if this
        // is that peer

        if (this.blocksPeerExclusivity.containsKey(blockHash)) {
            PeerAddress exclusivePeer = this.blocksPeerExclusivity.get(blockHash);
            boolean hasExclusivityForThisblock = exclusivePeer.equals(peer);
            if (hasExclusivityForThisblock) {
                // There is EXCLUSIVITY on this block, and the Exclusivity is for this Peer. WE assign it:
                log.debug("Downloading Block {} : Peer {} assigned (has exclusivity on this Block)",
                        request.getBlockHash(), request.getPeerAddress());
                return Optional.of(new DownloadResponse(request));
            } else {
                // There is EXCLUSIVITY on this block, but the Exclusivity is for ANOTHER Peer. We reject it:
                log.trace("Downloading Block {} : Peer {} discarded (Peer {} has exclusivity on this Block)",
                        request.getBlockHash(), request.getPeerAddress(), exclusivePeer);
                return Optional.of(new DownloadResponse(request, DownloadResponse.DownloadResponseState.OTHER_PEER_WITH_EXCLUSIVITY));
            }
        }

        // We Check PRIORITY:
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
                    result = Optional.of(new DownloadResponse(request, DownloadResponse.DownloadResponseState.OTHER_PEER_WITH_PRIORITY));
                }
            }
            return result;
        }

        return result;
    }

}
