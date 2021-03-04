package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.PartialBlockTXsMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a PARTIAL_BLOCK_TXS Message is received from a Remote Peer.
 *
 * (This is not an "Official" message in the Bitcoin Protocol. It's been used here in order to support
 * downloading Big Blocks)
 */
public final class PartialBlockTxsMsgReceivedEvent extends MsgReceivedEvent<PartialBlockTXsMsg> {
    public PartialBlockTxsMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockTXsMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
