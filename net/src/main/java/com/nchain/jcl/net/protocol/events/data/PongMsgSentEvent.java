package com.nchain.jcl.net.protocol.events.data;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.PongMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a PONG Message is sent to a remote Peer.
 */
public final class PongMsgSentEvent extends MsgSentEvent<PongMsg> {
    public PongMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<PongMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
