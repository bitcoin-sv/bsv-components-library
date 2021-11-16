package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class PrefilledTxMsg extends Message {

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
    public PrefilledTransactionMsgBuilder toBuilder() {
        return new PrefilledTransactionMsgBuilder()
                        .index(this.index)
                        .transaction(this.transaction);
    }

    public static class PrefilledTransactionMsgBuilder extends MessageBuilder {
        private VarIntMsg index;
        private TxMsg transaction;

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
