package io.bitcoinsv.bsvcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.Message;

import java.io.Serializable;


/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class PrefilledTxMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "prefilledTransaction";

    private final VarIntMsg index;
    private final TxMsg transaction;


    public PrefilledTxMsg(VarIntMsg index, TxMsg transaction) {
        this.index = index;
        this.transaction = transaction;
        init();
    }

    public static PrefilledTransactionMsgBuilder builder() {
        return new PrefilledTransactionMsgBuilder();
    }

    public VarIntMsg getIndex() {
        return index;
    }

    public TxMsg getTransaction() {
        return transaction;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return index.calculateLength() + transaction.calculateLength();
    }

    @Override
    protected void validateMessage() {
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PrefilledTxMsg other = (PrefilledTxMsg) obj;
        return Objects.equal(this.index, other.index)
                && Objects.equal(this.transaction, other.transaction);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.index, this.transaction);
    }

    public PrefilledTransactionMsgBuilder toBuilder() {
        return new PrefilledTransactionMsgBuilder()
                        .index(this.index)
                        .transaction(this.transaction);
    }

    /**
     * Builder
     */
    public static class PrefilledTransactionMsgBuilder {
        private VarIntMsg index;
        private TxMsg transaction;

        public PrefilledTransactionMsgBuilder() {}

        public PrefilledTransactionMsgBuilder index(VarIntMsg index) {
            this.index = index;
            return this;
        }

        public PrefilledTransactionMsgBuilder transaction(TxMsg transaction) {
            this.transaction = transaction;
            return this;
        }

        public PrefilledTxMsg build() {
            return new PrefilledTxMsg(index, transaction);
        }
    }
}
