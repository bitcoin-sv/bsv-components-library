package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.BlockMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a BLOCK Message is sent to a remote Peer.
 */
public final class BlockMsgSentEvent extends MsgSentEvent<BlockMsg> {
    public BlockMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<BlockMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
