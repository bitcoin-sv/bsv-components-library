package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.AddrMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a ADDR Message is received from a Remote Peer.
 */
public final class AddrMsgReceivedEvent extends MsgReceivedEvent<AddrMsg> {
    public AddrMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<AddrMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
