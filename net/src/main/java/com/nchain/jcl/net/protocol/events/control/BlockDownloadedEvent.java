package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.P2PEvent;
import com.nchain.jcl.net.protocol.events.data.MsgReceivedEvent;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.tools.events.Event;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 15:46
 *
 * An Event triggered when a Block has been fully downloaded, including its TXs. Since a Block could potentially
 * use more memory than the HW available, this Event only notifies the FACT that the block has been downloaded, and
 * also provides some info about it (like the Block Header, or the Peer it's been downloaded from).
 *
 * If you need to process the actual content of the block while it's being downloaded, you can listen to other
 * events, like the following:
 *
 * @see LiteBlockDownloadedEvent
 * @see MsgReceivedEvent (when the Msg is of type PartialBlockHeaderMSg or PartialBlockTXsMsg)
 *
 */
public final class BlockDownloadedEvent extends P2PEvent {
    private final PeerAddress peerAddress;
    private final BlockHeaderMsg blockHeader;
    private final Duration downloadingTime;
    private final Long blockSize;

    public BlockDownloadedEvent(PeerAddress peerAddress, BlockHeaderMsg blockHeader, Duration downloadingTime, Long blockSize) {
        this.peerAddress = peerAddress;
        this.blockHeader = blockHeader;
        this.downloadingTime = downloadingTime;
        this.blockSize = blockSize;
    }

    public PeerAddress getPeerAddress()     { return this.peerAddress; }
    public BlockHeaderMsg getBlockHeader()  { return this.blockHeader; }
    public Duration getDownloadingTime()    { return this.downloadingTime; }
    public Long getBlockSize()              { return this.blockSize; }

    @Override
    public String toString() {
        return "BlockDownloadedEvent(peerAddress=" + this.getPeerAddress() + ", blockHeader=" + this.getBlockHeader() + ", downloadingTime=" + this.getDownloadingTime() + ", blockSize=" + this.getBlockSize() + ")";
    }
}
