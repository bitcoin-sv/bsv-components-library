package io.bitcoinsv.bsvcl.net.protocol.events.data;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.FeeFilterMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a FEE_FILTER Message is received from a Remote Peer.
 */
public final class FeeMsgReceivedEvent extends MsgReceivedEvent<FeeFilterMsg> {
    public FeeMsgReceivedEvent(PeerAddress peerAddress, BitcoinMsg<FeeFilterMsg> btcMsg) {
        super(peerAddress, btcMsg);
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
