package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockTransactionsMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a @{@link com.nchain.jcl.net.protocol.messages.BlockTransactionsMsg} Message is sent to a remote Peer.
 * </p>
 */
public final class BlockTransactionsMsgSentEvent extends MsgSentEvent<BlockTransactionsMsg> {
    public BlockTransactionsMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<BlockTransactionsMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
