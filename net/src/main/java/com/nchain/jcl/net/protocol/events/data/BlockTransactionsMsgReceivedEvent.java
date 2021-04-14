package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockTransactionsMsg;
import com.nchain.jcl.net.protocol.messages.BlockTransactionsRequestMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a @{@link BlockTransactionsRequestMsg} Message is received from a Remote Peer.
 */
public final class BlockTransactionsMsgReceivedEvent extends MsgReceivedEvent<BlockTransactionsMsg> {
    public BlockTransactionsMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<BlockTransactionsMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
