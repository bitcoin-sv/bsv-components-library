package io.bitcoinsv.jcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.FeeFilterMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a FEE_FILTER Message is sent to a remote Peer.
 */
public final class FeeMsgSentEvent extends MsgSentEvent<FeeFilterMsg> {
    public FeeMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<FeeFilterMsg> btcMsg) {
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
