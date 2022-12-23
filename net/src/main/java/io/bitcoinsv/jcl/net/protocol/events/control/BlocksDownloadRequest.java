package io.bitcoinsv.jcl.net.protocol.events.control;


import com.google.common.base.Objects;
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

    // If specified, the blocks will ONLY be downloaded from this Peer:
    private final PeerAddress fromThisPeerOnly;

    // If specified, the blocks will be downloaded from these Pers if possible, before others:
    private final PeerAddress fromThisPeerPreferably;

    // If specified, then these blocks will always be downloaded even if the Handler is in PAUSED (Restrictive) Mode
    private final boolean forceDownload;

    public BlocksDownloadRequest(List<String> blockHashes,
                                 boolean withPriority,
                                 boolean forceDownload,
                                 PeerAddress fromThisPeerOnly,
                                 PeerAddress fromThisPeerPreferably) {
        this.blockHashes = blockHashes;
        this.withPriority = withPriority;
        this.forceDownload = forceDownload;
        this.fromThisPeerOnly = fromThisPeerOnly;
        this.fromThisPeerPreferably = fromThisPeerPreferably;
    }

    public BlocksDownloadRequest(List<String> blockHashes, boolean withPriority) {
        this(blockHashes, withPriority, false, null, null);
    }

    public List<String> getBlockHashes()            { return this.blockHashes; }
    public boolean isWithPriority()                 { return this.withPriority;}
    public boolean isForceDownload()                { return this.forceDownload;}
    public PeerAddress getFromThisPeerOnly()        { return this.fromThisPeerOnly;}
    public PeerAddress getFromThisPeerPreferably()  { return this.fromThisPeerPreferably;}

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        result
                .append("BlocksDownloadRequest(blockHashes=")
                .append(this.getBlockHashes())
                .append(", withPriority=" + withPriority);
        if (this.forceDownload) {
            result.append(" [downloaded forced] ");
        }
        if (this.fromThisPeerOnly != null) {
            result.append(", fromThisPeerOnly: " + this.fromThisPeerOnly);
        }
        if (this.fromThisPeerPreferably != null) {
            result.append(". fromThisPeerPreferable: " + this.fromThisPeerPreferably);
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BlocksDownloadRequest other = (BlocksDownloadRequest) obj;
        return Objects.equal(this.blockHashes, other.blockHashes)
                && Objects.equal(this.withPriority, other.withPriority)
                && Objects.equal(this.fromThisPeerOnly, other.fromThisPeerOnly)
                && Objects.equal(this.fromThisPeerPreferably, other.fromThisPeerPreferably)
                && Objects.equal(this.forceDownload, other.forceDownload);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), blockHashes, withPriority, fromThisPeerOnly, fromThisPeerPreferably, forceDownload);
    }
}