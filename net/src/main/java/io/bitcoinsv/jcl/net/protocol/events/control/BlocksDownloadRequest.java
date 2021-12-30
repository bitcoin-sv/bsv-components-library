package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:19
 *
 * A Request for Downloading a Block
 */
public final class BlocksDownloadRequest extends P2PRequest {
    // List of Block Hashes (HEX) to download
    private final List<String> blockHashes;
    // If true, these Blocks will be pushed to the BEGINNING of the Pending Pool, so they are picked up sooner
    private final boolean withPriority;
    // If specified, the blocks
    private final PeerAddress fromThisPeerOnly;
    private final PeerAddress fromThisPeerPreferably;

    public BlocksDownloadRequest(List<String> blockHashes, boolean withPriority,
                                 PeerAddress fromThisPeerOnly,
                                 PeerAddress fromThisPeerPreferably) {
        this.blockHashes = blockHashes;
        this.withPriority = withPriority;
        this.fromThisPeerOnly = fromThisPeerOnly;
        this.fromThisPeerPreferably = fromThisPeerPreferably;
    }

    public BlocksDownloadRequest(List<String> blockHashes, boolean withPriority) {
        this(blockHashes, withPriority, null, null);
    }

    public List<String> getBlockHashes()            { return this.blockHashes; }
    public boolean isWithPriority()                 { return this.withPriority;}
    public PeerAddress getFromThisPeerOnly()        { return this.fromThisPeerOnly;}
    public PeerAddress getFromThisPeerPreferably()  { return this.fromThisPeerPreferably;}

    @Override
    public String toString() {
        String result = "BlocksDownloadRequest(blockHashes=" + this.getBlockHashes() + ", withPriority=" + withPriority;
        if (this.fromThisPeerOnly != null) {
            result += ", fromThisPeerOnly: " + this.fromThisPeerOnly;
        }
        if (this.fromThisPeerPreferably != null) {
            result += ". fromThisPeerPreferable: " + this.fromThisPeerPreferably;
        }
        result += ")";
        return result;
    }
}
