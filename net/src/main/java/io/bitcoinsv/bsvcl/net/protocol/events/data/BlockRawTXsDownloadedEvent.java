package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.events.control.BlockDownloadedEvent;
import io.bitcoinsv.bsvcl.net.protocol.messages.PartialBlockRawTxMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-29 13:22
 *
 * An Event triggered when a Set of TXs from a Block has been downloaded. If you want to get notified when all the
 * TXs have been downloaded and the block has been fully download, use the BlockDownloadedEvent.
 *
 * NOTE: This event stores unserialized raw txs which contains only the hash
 *
 * @see BlockDownloadedEvent
 * @see LiteBlockDownloadedEvent
 */
public final class BlockRawTXsDownloadedEvent extends MsgReceivedEvent<PartialBlockRawTxMsg> {
    public BlockRawTXsDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockRawTxMsg> blockTxsMsg) {
        super(peerAddress, blockTxsMsg);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
