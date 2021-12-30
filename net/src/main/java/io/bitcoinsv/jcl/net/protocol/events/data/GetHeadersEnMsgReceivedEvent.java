package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.GetHeadersEnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a GET_HEADERS_EN Message is received from a Remote Peer.
 */
public final class GetHeadersEnMsgReceivedEvent extends MsgReceivedEvent<GetHeadersEnMsg> {
    public GetHeadersEnMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<GetHeadersEnMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}