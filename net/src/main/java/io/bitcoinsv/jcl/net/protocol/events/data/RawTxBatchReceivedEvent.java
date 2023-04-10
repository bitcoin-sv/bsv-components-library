package io.bitcoinsv.jcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.RawTxBatchMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

public final class RawTxBatchReceivedEvent extends MsgReceivedEvent<RawTxBatchMsg> {
    public RawTxBatchReceivedEvent(PeerAddress peerAddress, BitcoinMsg<RawTxBatchMsg> blockTxsMsg) {
        super(peerAddress, blockTxsMsg);
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