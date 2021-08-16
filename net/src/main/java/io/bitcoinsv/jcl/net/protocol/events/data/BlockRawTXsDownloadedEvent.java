/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.events.control.BlockDownloadedEvent;
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockRawTXsMsg;
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
 * NOTE: This event stores Transactions in RAW format (byte array), and it might contain partial Txs inside. So
 * in order to reconstruct the Txs, yo¡ll have to either develop a try-catch mechanism to deserialize them until
 * you come accross the last one which is not completed, or you wait until you get all events like this one from
 * the block, so you can reconstruct it completely.
 *
 * @see BlockDownloadedEvent
 * @see LiteBlockDownloadedEvent
 */
public final class BlockRawTXsDownloadedEvent extends MsgReceivedEvent<PartialBlockRawTXsMsg> {
    public BlockRawTXsDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockRawTXsMsg> blockTxsMsg) {
        super(peerAddress, blockTxsMsg);
    }
}
