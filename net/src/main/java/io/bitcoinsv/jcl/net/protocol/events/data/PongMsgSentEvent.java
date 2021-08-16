/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.PongMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

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
