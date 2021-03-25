package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockTransactionsRequestMsg;
import com.nchain.jcl.net.protocol.messages.GetHeadersMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a BLOCK TRANSACTIONS REQUEST Message is received from a Remote Peer.
 */
public final class BlockTransactionsRequestMsgReceivedEvent extends MsgReceivedEvent<BlockTransactionsRequestMsg> {
    public BlockTransactionsRequestMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<BlockTransactionsRequestMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
