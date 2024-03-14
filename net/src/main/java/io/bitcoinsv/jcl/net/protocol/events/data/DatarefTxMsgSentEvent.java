package io.bitcoinsv.jcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.DatarefTxMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * Copyright (c) 2024 nChain Ltd
 * <br>
 * An Event triggered when a DATAREF TX Message is sent to a remote Peer.
 *
 * @author nChain Ltd
 */
public final class DatarefTxMsgSentEvent extends MsgSentEvent<DatarefTxMsg> {
    public DatarefTxMsgSentEvent(PeerAddress peerAddress, BitcoinMsg<DatarefTxMsg> btcMsg) {
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
