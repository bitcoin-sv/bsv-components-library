package io.bitcoinsv.jcl.net.protocol.events.control;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a Message to an specific Peer
 *
 * THE MESSAGE WILL ONLY BE SENT IF THE PEER IS HANDSHAKED
 */
public final class SendMsgHandshakedRequest extends SendMsgRequest {
    public SendMsgHandshakedRequest(PeerAddress peerAddress, BitcoinMsg<?> btcMsg) {
        super(peerAddress, btcMsg);
    }

    @Override
    public String toString() {
        return "SendMsgHandshakeRequest[" + super.toString() + "]";
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
