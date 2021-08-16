/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.data;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.RawBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a BLOCK Message is received from a Remote Peer.
 */
public final class RawBlockMsgReceivedEvent extends MsgReceivedEvent<RawBlockMsg> {
    public RawBlockMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<RawBlockMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
