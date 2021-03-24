package com.nchain.jcl.net.protocol.events.control;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.CompleteBlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.tools.events.Event;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-29 13:22
 *
 * An Event triggered when a Set of TXs from a Block has been downloaded. If you want to get notified when all the
 * TXs have been downloaded and the block has been fully download, use the BlockDownloadedEvent.
 *
 * @see BlockDownloadedEvent
 * @see LiteBlockDownloadedEvent
 */
public final class BlockTXsDownloadedEvent extends Event {
    private final PeerAddress peerAddress;
    private final CompleteBlockHeaderMsg blockHeaderMsg;
    private final List<TxMsg> txsMsg;

    public BlockTXsDownloadedEvent(PeerAddress peerAddress, CompleteBlockHeaderMsg blockHeaderMsg, List<TxMsg> txsMsg) {
        this.peerAddress = peerAddress;
        this.blockHeaderMsg = blockHeaderMsg;
        this.txsMsg = txsMsg;
    }

    public PeerAddress getPeerAddress()         { return this.peerAddress; }
    public CompleteBlockHeaderMsg getBlockHeaderMsg()   { return this.blockHeaderMsg; }
    public List<TxMsg> getTxsMsg()              { return this.txsMsg; }

    @Override
    public String toString() {
        return "BlockTXsDownloadedEvent(peerAddress=" + this.getPeerAddress() + ", blockHeaderMsg=" + this.getBlockHeaderMsg() + ", txsMsg=" + this.getTxsMsg() + ")";
    }
}
