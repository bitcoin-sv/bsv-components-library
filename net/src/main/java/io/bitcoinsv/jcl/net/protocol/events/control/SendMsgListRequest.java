package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a List of Messages to an specific Peer
 */
public final class SendMsgListRequest extends P2PRequest {
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
