package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockTransactionsRequestMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a @{@link com.nchain.jcl.net.protocol.messages.BlockTransactionsRequestMsg} Message is sent to a remote Peer.
 * </p>
 */
public final class BlockTransactionsRequestMsgSentEvent extends MsgSentEvent<BlockTransactionsRequestMsg> {
    public BlockTransactionsRequestMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<BlockTransactionsRequestMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
