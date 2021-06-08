package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.net.network.events.P2PRequest;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:19
 *
 * A Request for Canceling the Downloadingfor a set of Blocks.
 * the blocks which are currently in the middle of the downloading might be or not be broadcasted (see implementation
 * in the BlockdownloaderHandlerImpl).
 *
 * @see com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerImpl
 */
public final class BlocksCancelDownloadRequest extends P2PRequest {
    private final List<String> blockHashes;

    public BlocksCancelDownloadRequest(List<String> blockHashes) {
        this.blockHashes = blockHashes;
    }

    public List<String> getBlockHashes() { return this.blockHashes; }

    @Override
    public String toString() {
        return "BlocksCancelDownloadRequest(blockHashes=" + this.getBlockHashes() + ")";
    }
}
