package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a BLOCK Message is received from a Remote Peer.
 */
public final class BlockMsgReceivedEvent extends MsgReceivedEvent<BlockMsg> {
    public BlockMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<BlockMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
