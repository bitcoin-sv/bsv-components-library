package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;

public final class CompactTransactionMsg extends Message implements Serializable {
    public static final String MESSAGE_TYPE = "CompactTransaction";

    protected final HashMsg txId;
    protected final boolean isIndependent;

    public CompactTransactionMsg(Sha256Hash txId, boolean isIndependent) {
        this.txId = HashMsg.builder().hash(txId.getBytes()).build();
        this.isIndependent = isIndependent;
        init();
    }

    public CompactTransactionMsg(HashMsg txId, boolean isIndependent) {
        this.txId = txId;
        this.isIndependent = isIndependent;
        init();
    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    protected long calculateLength() {
        return (txId.getLengthInBytes() + 1);
    }

    @Override
    protected void validateMessage() { }

    @Override
    public String toString() {
        return ("CompactTransactionMsg(txId=" + txId + ", isIndependent=" + isIndependent + ")");
    }

    public HashMsg getTxId() { return txId; }
    public boolean isIndependent() { return isIndependent; }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        CompactTransactionMsg other = (CompactTransactionMsg) obj;
        return isIndependent == other.isIndependent && Objects.equal(txId, other.txId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), txId, isIndependent);
    }
}
