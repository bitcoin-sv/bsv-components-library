package io.bitcoinsv.bsvcl.net.protocol.events.control;


import io.bitcoinsv.bsvcl.net.protocol.messages.common.StreamRequest;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.events.P2PRequest;

import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * THE MESSAGE WILL ONLY BE SENT IF THE PEER IS HANDSHAKED
 */
public final class SendMsgStreamHandshakeRequest extends P2PRequest {

    private final PeerAddress peerAddress;
    private final StreamRequest streamRequest;

    public SendMsgStreamHandshakeRequest(PeerAddress peerAddress, StreamRequest streamRequest) {
        this.peerAddress = peerAddress;
        this.streamRequest = streamRequest;
    }

    @Override
    public String toString() {
        return "SendMsgStreamHandshakeRequest[" + super.toString() + "])";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }

    public PeerAddress getPeerAddress() {
        return peerAddress;
    }

    public StreamRequest getStreamRequest() {
        return streamRequest;
    }
}
