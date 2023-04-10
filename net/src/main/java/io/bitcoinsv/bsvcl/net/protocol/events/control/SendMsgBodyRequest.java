package io.bitcoinsv.bsvcl.net.protocol.events.control;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.events.P2PRequest;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a Message to an specific Peer.
 * Unlike the SendMsgRequest, this Request does not specify the Header of the Message, only the Body. The Header will
 * be automatically created at the moment of sending the mesage...
 */
public class SendMsgBodyRequest extends P2PRequest {
    private final PeerAddress peerAddress;
    private final BodyMessage msgBody;

    public SendMsgBodyRequest(PeerAddress peerAddress, BodyMessage msgBody
    ) {
        this.peerAddress = peerAddress;
        this.msgBody = msgBody;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }
    public BodyMessage getMsgBody()    { return this.msgBody; }

    @Override
    public String toString() {
        return "SendMsgBodyRequest(peerAddress=" + this.getPeerAddress() + ", msgBody=" + this.getMsgBody() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        SendMsgBodyRequest other = (SendMsgBodyRequest) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.msgBody, other.msgBody);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, msgBody);
    }
}
