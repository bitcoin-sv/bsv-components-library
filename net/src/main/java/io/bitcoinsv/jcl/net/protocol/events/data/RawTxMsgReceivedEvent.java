package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.RawTxMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Raw TX Message is received from a Remote Peer.
 */
public final class RawTxMsgReceivedEvent extends MsgReceivedEvent<RawTxMsg> {
    public RawTxMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<RawTxMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
