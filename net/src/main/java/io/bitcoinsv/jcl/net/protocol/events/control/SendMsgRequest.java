/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a Message to an specific Peer
 */
public final class SendMsgRequest extends P2PRequest {
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