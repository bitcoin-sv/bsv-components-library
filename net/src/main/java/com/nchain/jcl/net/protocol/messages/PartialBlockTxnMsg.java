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
    private final int order;

    public PartialBlockTxnMsg(HashMsg blockHash, List<TxMsg> transactions, int order, long payloadChecksum) {
        super(payloadChecksum);
        this.blockHash = blockHash;
        this.transactions = transactions;
        this.order = order;
        init();
    }

    public static PartialBlockTxnBuilder builder() {
        return new PartialBlockTxnBuilder();
    }

    public HashMsg getBlockHash()           { return blockHash; }
    public List<TxMsg> getTransactions()    { return transactions; }
    public int getOrder()                   { return order; }

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

    @Override
    public PartialBlockTxnBuilder toBuilder() {
        return new PartialBlockTxnBuilder()
                        .blockHash(this.blockHash)
                        .transactions(this.transactions)
                        .order(this.order);
    }

    /**
     * Builder
     */
    public static class PartialBlockTxnBuilder extends MessageBuilder{
        private HashMsg blockHash;
        private List<TxMsg> transactions;
        private int order;

        public PartialBlockTxnBuilder blockHash(HashMsg blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public PartialBlockTxnBuilder transactions(List<TxMsg> transactions) {
            this.transactions = transactions;
            return this;
        }

        public PartialBlockTxnBuilder order(int order) {
            this.order = order;
            return this;
        }

        public PartialBlockTxnMsg build() {
            return new PartialBlockTxnMsg(blockHash, transactions, order, super.payloadChecksum);
        }
    }
}
