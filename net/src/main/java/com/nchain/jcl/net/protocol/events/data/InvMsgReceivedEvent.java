package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.InvMessage;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a INV Message is received from a Remote Peer.
 */
public final class InvMsgReceivedEvent extends MsgReceivedEvent<InvMessage> {
    public InvMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<InvMessage> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
