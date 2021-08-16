package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.events.control.BlockDownloadedEvent;
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockTXsMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

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
public final class BlockTXsDownloadedEvent extends MsgReceivedEvent<PartialBlockTXsMsg> {
    public BlockTXsDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockTXsMsg> blockTxsMsg) {
        super(peerAddress, blockTxsMsg);
    }
}
