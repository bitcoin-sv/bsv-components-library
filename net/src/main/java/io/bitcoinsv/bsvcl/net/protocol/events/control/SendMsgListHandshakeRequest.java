package io.bitcoinsv.bsvcl.net.protocol.events.control;


import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

import java.util.List;
import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * THE MESSAGE WILL ONLY BE SENT IF THE PEER IS HANDSHAKED
 */
public final class SendMsgListHandshakeRequest extends SendMsgListRequest {

    public SendMsgListHandshakeRequest(PeerAddress peerAddress, List<BitcoinMsg<?>> btcMsgs) {
        super(peerAddress, btcMsgs);
    }

    @Override
    public String toString() {
        return "SendMsgListHandshakeRequest[" + super.toString() + "])";
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
