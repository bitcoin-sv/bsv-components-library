package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.CompactBlockMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a COMPACT BLOCK Message is received from a remote Peer.
 * </p>
 */
public final class CompactBlockMsgReceivedEvent extends MsgSentEvent<CompactBlockMsg> {
    public CompactBlockMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<CompactBlockMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
