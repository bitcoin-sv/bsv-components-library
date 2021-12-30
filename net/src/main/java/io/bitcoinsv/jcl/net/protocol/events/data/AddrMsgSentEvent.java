package io.bitcoinsv.jcl.net.protocol.events.data;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.AddrMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a ADDR Message is sent to a remote Peer.
 */
public final class AddrMsgSentEvent extends MsgSentEvent<AddrMsg> {
    public AddrMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<AddrMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
