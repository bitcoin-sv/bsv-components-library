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
 */
public class SendMsgRequest extends P2PRequest {
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

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        SendMsgRequest other = (SendMsgRequest) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.btcMsg, other.btcMsg);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), peerAddress, btcMsg);
    }
}
