package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a TX Message is sent to a remote Peer.
 */
public final class TxMsgSentEvent extends MsgSentEvent<TxMsg> {
    public TxMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<TxMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
