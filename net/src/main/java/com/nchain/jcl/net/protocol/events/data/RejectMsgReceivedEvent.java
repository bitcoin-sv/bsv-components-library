package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.RejectMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a REJECT Message is received from a Remote Peer.
 */
public final class RejectMsgReceivedEvent extends MsgReceivedEvent<RejectMsg> {
    public RejectMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<RejectMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
