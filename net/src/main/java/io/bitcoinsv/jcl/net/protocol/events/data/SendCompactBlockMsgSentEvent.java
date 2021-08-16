package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.SendCompactBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a SEND COMPACT BLOCK Message is sent to a remote Peer.
 * </p>
 */
public final class SendCompactBlockMsgSentEvent extends MsgSentEvent<SendCompactBlockMsg> {
    public SendCompactBlockMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<SendCompactBlockMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
