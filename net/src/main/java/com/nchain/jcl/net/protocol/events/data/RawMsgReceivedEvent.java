package com.nchain.jcl.net.protocol.events.data;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.events.P2PEvent;
import com.nchain.jcl.net.protocol.messages.RawMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Raw Message is received from a Remote Peer.
 */
public class RawMsgReceivedEvent<T extends RawMsg> extends MsgReceivedEvent {

    public RawMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<T> btcMsg) {
        super(peerAddress, btcMsg);
    }

    @Override
    public String toString() {
        return "Raw " + super.toString();
    }
}
