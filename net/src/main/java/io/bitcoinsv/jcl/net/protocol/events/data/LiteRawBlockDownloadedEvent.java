package io.bitcoinsv.jcl.net.protocol.events.data;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PEvent;
import io.bitcoinsv.jcl.net.protocol.events.control.BlockDownloadedEvent;
import io.bitcoinsv.jcl.net.protocol.messages.RawBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 15:46
 *
 * An Event triggered when a "Lite" Block has been downloaded. A Lite block is a block which size is small enough
 * to be put into memory without risking running out of it.
 *
 * NOTE:
 * When listening for Events regarding Blocks downloaded, the best approach is to listen to "BlockDownloadedEvent"
 * instead, which is always triggered regardless of the block size. This event here thought is only triggered when
 * the Block is NOT Big
 *
 * @see BlockDownloadedEvent
 */
public final class LiteRawBlockDownloadedEvent extends P2PEvent {
    private final PeerAddress peerAddress;
    private final BitcoinMsg<RawBlockMsg> block; // Whole Downloaded Block.
    private final Duration downloadingTime;

    public LiteRawBlockDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<RawBlockMsg> block, Duration downloadingTime) {
        this.peerAddress = peerAddress;
        this.block = block;
        this.downloadingTime = downloadingTime;
    }

    public PeerAddress getPeerAddress()         { return this.peerAddress; }
    public BitcoinMsg<RawBlockMsg> getBlock()   { return this.block; }
    public Duration getDownloadingTime()        { return this.downloadingTime; }

    @Override
    public String toString() {
        return "LiteBlockDownloadedEvent(peerAddress=" + this.getPeerAddress() + ", block=" + this.getBlock() + ", downloadingTime=" + this.getDownloadingTime() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        LiteRawBlockDownloadedEvent other = (LiteRawBlockDownloadedEvent) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.block, other.block)
                && Objects.equal(this.downloadingTime, other.downloadingTime);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, block, downloadingTime);
    }
}
