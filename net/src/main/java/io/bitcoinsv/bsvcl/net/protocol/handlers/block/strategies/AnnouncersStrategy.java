package io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.protocol.events.data.HeadersMsgReceivedEvent;
import io.bitcoinsv.bsvcl.net.protocol.events.data.InvMsgReceivedEvent;
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;
import io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg;

import io.bitcoinsv.bsvcl.tools.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Download Strategy that downloads blocks from those Peers that have prevoouly announced them.
 * - In case some of those "announcers" are not available, it also uses a "bestMatchNotAvailableAction" Polocy to
 *   decide which one to choose instead.
 * - In case no Announcers at all are found, it uses a "noBestMatchAction" policy to decide what to do next.
 */
public class AnnouncersStrategy extends PriorityStrategy implements BlockDownloadStrategy {

    private static Logger log = LoggerFactory.getLogger(AnnouncersStrategy.class);
    private BlockDownloaderHandlerConfig.BestMatchNotAvailableAction bestMatchNotAvailableAction = BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE;
    private BlockDownloaderHandlerConfig.NoBestMatchAction noBestMatchAction                     = BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE;

    // Blocks announced by Peer: [Key: peer. Value: List of Blocks announced by this peer]
    private Map<PeerAddress, Set<String>> blockAnnouncements = new ConcurrentHashMap<>();

    /** Constructor */
    public AnnouncersStrategy(EventBus eventBus,
                              BlockDownloaderHandlerConfig.BestMatchNotAvailableAction bestMatchNotAvailableAction,
                              BlockDownloaderHandlerConfig.NoBestMatchAction noBestMatchAction) {
        super(BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS, eventBus);
        this.eventBus = eventBus;
        this.bestMatchNotAvailableAction = bestMatchNotAvailableAction;
        this.noBestMatchAction = noBestMatchAction;

        // We get notified about a block being announced:
        this.eventBus.subscribe(InvMsgReceivedEvent.class, e -> this.onInvMsgReceived((InvMsgReceivedEvent) e));
        this.eventBus.subscribe(HeadersMsgReceivedEvent.class, e -> this.onHeadersMsgReceived((HeadersMsgReceivedEvent) e));
    }

    public void registerBlockAnnouncement(String blockHash, PeerAddress peerAddress) {
        blockAnnouncements.merge(peerAddress, new HashSet<>() {{add(blockHash);}}, (prev, one) -> {
            prev.addAll(one);
            return prev;
        });
    }

    private boolean isBlockAnnouncedBy(String blockHash, PeerAddress peerAddress) {
        return blockAnnouncements.containsKey(peerAddress) && blockAnnouncements.get(peerAddress).contains(blockHash);
    }

    private boolean isBlockAnnouncedBy(String blockHash, List<PeerAddress> peerAddress) {
        return peerAddress.stream().anyMatch(p -> isBlockAnnouncedBy (blockHash, p));
    }

    // Event Handler:
    // We register the Peers that are announcing Blocks:
    private void onInvMsgReceived(InvMsgReceivedEvent event) {
        event.getBtcMsg().getBody().getInvVectorList().stream()
                .filter(item -> item.getType().equals(InventoryVectorMsg.VectorType.MSG_BLOCK))
                .forEach(item -> {
                    String blockHash = Sha256Hash.wrapReversed(item.getHashMsg().getHashBytes()).toString();
                    log.trace("Block {} announced by [{}] in a INV msg", blockHash, event.getPeerAddress());
                    registerBlockAnnouncement(blockHash, event.getPeerAddress());
                });
    }

    // Event Handler:
    // We register the Peers that are announcing blocks:
    private void onHeadersMsgReceived(HeadersMsgReceivedEvent event) {
        event.getBtcMsg().getBody().getBlockHeaderMsgList()
                .forEach(h -> {
                    log.trace("Block {} announced by [{}] in a HEADERS msg", h.getHash().toString(), event.getPeerAddress());
                    registerBlockAnnouncement(h.getHash().toString(), event.getPeerAddress());
                });
    }


    @Override
    public Optional<DownloadResponse> requestDownload(
            DownloadRequest request,
            List<PeerAddress> availablePeers,
            List<PeerAddress> notAvailablePeers) {

        String blockHash = request.getBlockHash();
        PeerAddress peerAddress = request.getPeerAddress();

        // First we Asked the parent Strategy, and we continue only of the parent has not provided answer (Empty):
        Optional<DownloadResponse> result = super.requestDownload(request, availablePeers, notAvailablePeers);
        if (result.isEmpty()) {
            if (isBlockAnnouncedBy(blockHash, peerAddress)) {
                // Announced by this Peer. We assign it
                log.debug("Downloading Block {} : Peer {} assigned (From Announcers: announced by this Peer)", blockHash, peerAddress);
                return Optional.of(new DownloadResponse(request));
            } else if (isBlockAnnouncedBy(blockHash, availablePeers)) {
                // Announced by OTHER available Peer. We skip this one (Return false)
                log.trace("Downloading Block {} : Peer {} discarded (From Announcers: announced by other available Peer)", blockHash, peerAddress);
                return Optional.of(new DownloadResponse(request, DownloadResponse.DownloadResponseState.OTHER_PEER_ANNOUNCER));
            } else if (isBlockAnnouncedBy(blockHash, notAvailablePeers)) {
                // Block has been announced by a Peer that is NOT available. We do based on Action defined:
                if (bestMatchNotAvailableAction.equals(BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE)) {
                    // It can be Download by ANYBODY. We assign it:
                    log.debug("Downloading Block {} : Peer {} assigned (From Announcers: announced by a Not available Peer, but anyone can download)", blockHash, peerAddress);
                    return Optional.of(new DownloadResponse(request));
                } else {
                    // It cannot be Assigned to this Peer:
                    log.trace("Downloading Block {} : Peer {} discarded (From Announcers: not announced, announced by a Not available Peer and we must Wait)", blockHash, peerAddress);
                    return Optional.of(new DownloadResponse(request, DownloadResponse.DownloadResponseState.OTHER_PEER_ANNOUNCER));
                }
            } else {
                // Block has NOT been announced by ANY Peer at all. We do based on Action defined:
                if (noBestMatchAction.equals(BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE)) {
                    log.trace("Downloading Block {} : Peer {} assigned (From Announcers: not announced by any peer, anybody can download)", blockHash, peerAddress);
                    // It can be Download by ANYBODY. We assign it:
                    return Optional.of(new DownloadResponse(request));
                } else {
                    // It cannot be Assigned to this Peer:
                    log.trace("Downloading Block {} : Peer {} discarded (From Announcers: not announced by any peer, we must Wait)", blockHash, peerAddress);
                    return Optional.of(new DownloadResponse(request, DownloadResponse.DownloadResponseState.NO_ANNOUNCERS));
                }
            }
        }
        return result;
    }
}
