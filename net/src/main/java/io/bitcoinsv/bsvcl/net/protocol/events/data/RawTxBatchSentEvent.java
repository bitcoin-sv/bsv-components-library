package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.protocol.messages.RawTxBatchMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;

public final class RawTxBatchSentEvent extends MsgSentEvent<RawTxBatchMsg> {
    public RawTxBatchSentEvent(PeerAddress peerAddress, BitcoinMsg<RawTxBatchMsg> btcMsg) {
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