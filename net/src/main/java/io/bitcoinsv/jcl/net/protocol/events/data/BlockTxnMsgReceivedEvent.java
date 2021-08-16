package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.BlockTxnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.GetBlockTxnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a @{@link GetBlockTxnMsg} Message is received from a Remote Peer.
 */
public final class BlockTxnMsgReceivedEvent extends MsgReceivedEvent<BlockTxnMsg> {
    public BlockTxnMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<BlockTxnMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
