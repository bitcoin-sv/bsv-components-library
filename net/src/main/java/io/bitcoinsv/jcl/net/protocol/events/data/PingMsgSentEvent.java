package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.PingMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a PONG Message is sent to a remote Peer.
 */
public final class PingMsgSentEvent extends MsgSentEvent<PingMsg> {
    public PingMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<PingMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}