package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.PingMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a PING Message is received from a Remote Peer.
 */
public final class PingMsgReceivedEvent extends MsgReceivedEvent<PingMsg> {
    public PingMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<PingMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
