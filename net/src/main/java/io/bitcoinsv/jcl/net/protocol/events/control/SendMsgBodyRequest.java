/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a Message to an specific Peer.
 * Unlike the SendMsgRequest, this Request does not specify the Header of the Message, only the Body. The Header will
 * be automatically created at the moment of sending the mesage...
 */
public final class SendMsgBodyRequest extends P2PRequest {
    private final PeerAddress peerAddress;
    private final Message msgBody;

    public SendMsgBodyRequest(PeerAddress peerAddress, Message msgBody
    ) {
        this.peerAddress = peerAddress;
        this.msgBody = msgBody;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public Message getMsgBody()    { return this.msgBody; }

    @Override
    public String toString() {
        return "SendMsgBodyRequest(peerAddress=" + this.getPeerAddress() + ", msgBody=" + this.getMsgBody() + ")";
    }
}
