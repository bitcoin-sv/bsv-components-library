package io.bitcoinsv.jcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.GetBlockTxnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An Event triggered when a @{@link GetBlockTxnMsg} Message is sent to a remote Peer.
 * </p>
 */
public final class GetBlockTxnMsgSentEvent extends MsgSentEvent<GetBlockTxnMsg> {
    public GetBlockTxnMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<GetBlockTxnMsg> btcMsg) {
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
