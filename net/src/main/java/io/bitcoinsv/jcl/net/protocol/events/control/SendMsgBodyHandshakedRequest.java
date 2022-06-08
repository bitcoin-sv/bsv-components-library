package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a Message to an specific Peer.
 * Unlike the SendMsgRequest, this Request does not specify the Header of the Message, only the Body. The Header will
 * be automatically created at the moment of sending the mesage...
 *
 * THE MESSAGE WILL ONLY BE SENT I FTHE PEER IS HANDSHAKED
 */
public final class SendMsgBodyHandshakedRequest extends SendMsgBodyRequest {
    public SendMsgBodyHandshakedRequest(PeerAddress peerAddress, BodyMessage msgBody) {
        super(peerAddress, msgBody);
    }

    @Override
    public String toString() {
        return "SendMsgBodyHandshakedRequest[" + super.toString() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
