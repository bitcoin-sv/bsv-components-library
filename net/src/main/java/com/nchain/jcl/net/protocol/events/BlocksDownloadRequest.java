package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.tools.events.Event;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:19
 *
 * A Request for Downloading a Block
 */
public final class BlocksDownloadRequest extends Event {
    private final List<String> blockHashes;

    public BlocksDownloadRequest(List<String> blockHashes) {
        this.blockHashes = blockHashes;
    }

    public List<String> getBlockHashes() { return this.blockHashes; }

    @Override
    public String toString() {
        return "BlocksDownloadRequest(blockHashes=" + this.getBlockHashes() + ")";
    }
}
