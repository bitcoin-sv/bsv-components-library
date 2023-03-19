package io.bitcoinsv.jcl.net.protocol.handlers.block.strategies;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig;
import io.bitcoinsv.jcl.tools.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.bitcoinsv.jcl.net.protocol.handlers.block.strategies.DownloadResponse.DownloadResponseState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Downbload Straegy where (After priorities logic is applied), all Downbload Request are approved. This
 * ensures that the blocks are always downloaded in order. This is mandatory for the IBD logic to work properly.
 */
public class IBDStrategy extends PriorityStrategy implements BlockDownloadStrategy {

    private static Logger log = LoggerFactory.getLogger(IBDStrategy.class);

    /** Constructor */
    public IBDStrategy(EventBus eventBus) {
        super(BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANYONE, eventBus);
        this.eventBus = eventBus;
    }

    @Override
    public Optional<DownloadResponse> requestDownload(
            DownloadRequest request,
            List<PeerAddress> availablePeers,
            List<PeerAddress> notAvailablePeers) {
        return Optional.of(new DownloadResponse(request));
    }
}