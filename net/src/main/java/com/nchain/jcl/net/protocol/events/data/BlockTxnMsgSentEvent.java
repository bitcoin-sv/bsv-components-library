package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockTxnMsg;
import com.nchain.jcl.net.protocol.messages.GetBlockTxnMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

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
}
