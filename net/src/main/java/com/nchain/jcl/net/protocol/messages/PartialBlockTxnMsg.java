package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 */
public class PartialBlockTxnMsg extends Message implements Serializable {
    public static final String MESSAGE_TYPE = "PartialBockTxn";

    private final HashMsg blockHash;
    private final List<TxMsg> transactions;

    public PartialBlockTxnMsg(HashMsg blockHash, List<TxMsg> transactions) {
        this.blockHash = blockHash;
        this.transactions = transactions;
        init();
    }

    public static PartialBlockTxnBuilder builder() {
        return new PartialBlockTxnBuilder();
    }

    public HashMsg getBlockHash() {
        return blockHash;
    }

    public List<TxMsg> getTransactions() {
        return transactions;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return blockHash.calculateLength()
            + VarIntMsg.builder()
                .value(transactions.size())
                .build()
                .calculateLength()
            + transactions.stream()
                .mapToLong(TxMsg::calculateLength)
                .sum();
    }

    @Override
    protected void validateMessage() {
    }

    public static class PartialBlockTxnBuilder {
        private HashMsg blockHash;
        private List<TxMsg> transactions;

        public PartialBlockTxnBuilder blockHash(HashMsg blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public PartialBlockTxnBuilder transactions(List<TxMsg> transactions) {
            this.transactions = transactions;
            return this;
        }

        public PartialBlockTxnMsg build() {
            return new PartialBlockTxnMsg(blockHash, transactions);
        }
    }
}
