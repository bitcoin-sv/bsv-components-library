package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockTxnMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.GetBlockTxnMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a @{@link GetBlockTxnMsg} Message is sent to a remote Peer.
 * </p>
 */
public final class BlockTxnMsgSentEvent extends MsgSentEvent<BlockTxnMsg> {
    public BlockTxnMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<BlockTxnMsg> btcMsg) {
        super(peerAddress, btcMsg);
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