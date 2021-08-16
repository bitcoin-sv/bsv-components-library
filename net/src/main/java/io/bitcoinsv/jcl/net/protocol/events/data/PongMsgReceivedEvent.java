package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.PongMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a PONG Message is received from a Remote Peer.
 */
public final class PongMsgReceivedEvent extends MsgReceivedEvent<PongMsg> {
    public PongMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<PongMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
