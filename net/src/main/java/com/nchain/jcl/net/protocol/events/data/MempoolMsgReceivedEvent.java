package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.MemPoolMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a MEMPOOL Message is received from a Remote Peer.
 */
public final class MempoolMsgReceivedEvent extends MsgReceivedEvent<MemPoolMsg> {
    public MempoolMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<MemPoolMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
