package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.RawTxBlockMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a BLOCK Message is received from a Remote Peer.
 */
public final class RawBlockMsgReceivedEvent extends MsgReceivedEvent<RawTxBlockMsg> {
    public RawBlockMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<RawTxBlockMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
