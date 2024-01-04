package io.bitcoinsv.jcl.net.protocol.handlers.block;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.tools.handlers.Handler;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations provided by the BlockDownloader Handler.
 */
public interface BlockDownloaderHandler extends Handler {
    String HANDLER_ID = "Download";

    @Override
    default String getId() { return HANDLER_ID; }

    /** Adds more Block Hashes to the list of Blocks to Download */
    void download(List<String> blockHashes);
    void download(List<String> blockHashes, boolean withPriority);
    void download(List<String> blockHashes, boolean withPriority, PeerAddress fromThisPeerOnly, PeerAddress fromThisPeerPreferably);

    /**
     * Cancels the download of the blocks given. If some of the blocks are already being download, those blocks will
     * get broadcasted
     */
    void cancelDownload(List<String> blockHashes);

    List<BlockPeerInfo> getPeers();
}
