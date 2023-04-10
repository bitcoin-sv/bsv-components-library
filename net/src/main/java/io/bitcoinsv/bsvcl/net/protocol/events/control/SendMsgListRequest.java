package io.bitcoinsv.bsvcl.net.protocol.events.control;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.events.P2PRequest;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event representing a Request to send a List of Messages to an specific Peer
 */
public class SendMsgListRequest extends P2PRequest {
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

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        SendMsgListRequest other = (SendMsgListRequest) obj;
        return Objects.equal(this.peerAddress, other.peerAddress)
                && Objects.equal(this.btcMsgs, other.btcMsgs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.peerAddress, this.btcMsgs);
    }
}
