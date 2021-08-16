package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.SendHeadersMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a SEND_HEADERS Message is received from a Remote Peer.
 */
public final class SendHeadersMsgReceivedEvent extends MsgReceivedEvent<SendHeadersMsg> {
    public SendHeadersMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<SendHeadersMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
