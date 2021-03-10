package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.GetdataMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a GET_DATA Message is received from a Remote Peer.
 */
public final class GetDataMsgReceivedEvent extends MsgReceivedEvent<GetdataMsg> {
    public GetDataMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<GetdataMsg> btcMsg) {
        super(peerAddress, btcMsg);
    }
}
