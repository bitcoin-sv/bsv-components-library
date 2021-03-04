package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a Message to an specific Peer
 */
public final class SendMsgRequest extends Event {
    private final PeerAddress peerAddress;
    private final BitcoinMsg<?> btcMsg;

    public SendMsgRequest(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
        this.peerAddress = peerAddress;
        this.btcMsg = btcMsg;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public BitcoinMsg<?> getBtcMsg()    { return this.btcMsg; }

    @Override
    public String toString() {
        return "SendMsgRequest(peerAddress=" + this.getPeerAddress() + ", btcMsg=" + this.getBtcMsg() + ")";
    }
}
