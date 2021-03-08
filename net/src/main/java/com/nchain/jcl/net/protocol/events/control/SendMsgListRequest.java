package com.nchain.jcl.net.protocol.events.control;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.events.Event;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a List of Messages to an specific Peer
 */
public final class SendMsgListRequest extends Event {
    private final PeerAddress peerAddress;
    private final List<BitcoinMsg<?>> btcMsgs;

    public SendMsgListRequest(PeerAddress peerAddress, List<BitcoinMsg<?>> btcMsgs) {
        this.peerAddress = peerAddress;
        this.btcMsgs = btcMsgs;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public List<BitcoinMsg<?>> getBtcMsgs()    { return this.btcMsgs; }

    @Override
    public String toString() {
        return "SendMsgListRequest(peerAddress=" + this.getPeerAddress() + ", " + this.getBtcMsgs().size() + " msgs)";
    }
}
