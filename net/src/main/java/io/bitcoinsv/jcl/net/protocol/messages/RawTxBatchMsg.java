package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class RawTxBatchMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "txbatch";

    private List<byte[]> txs;

    public RawTxBatchMsg(List<byte[]> txs, byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.txs = txs;
        init();
    }

    public List<byte[]> getTxs() {
        return txs;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return 4L + (txs.size() * 4L) + txs.stream().mapToInt(bytes -> bytes.length).sum();
    }

    @Override
    protected void validateMessage() {
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }

    @Override
    public BodyMessageBuilder toBuilder() {
        return new RawTxBatchMsgBuilder()
                .txs(txs);
    }

    @Override
    public String toString() {
        return "RawTxBatchMsg(txs=" + this.txs.size() + ")";
    }

    public static class RawTxBatchMsgBuilder extends BodyMessageBuilder {
        private List<byte[]> txs = new ArrayList<>();

        public RawTxBatchMsgBuilder txs(List<byte[]> txs) {
            this.txs = new ArrayList<>(txs);
            return this;
        }

        @Override
        public RawTxBatchMsg build() {
            return new RawTxBatchMsg(txs, super.extraBytes, super.checksum);
        }
    }
}