package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 */
public final class PartialBlockTxnMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "PartialBockTxn";

    // Original Header Msg: Included here in case the client of JCL receiving the partial Messages
    // wants to calculate and verify the checksum of the original message:
    private final HeaderMsg headerMsg;
    private final HashMsg blockHash;
    private final List<TxMsg> transactions;
    private final int order;

    public PartialBlockTxnMsg(HeaderMsg headerMsg,  HashMsg blockHash, List<TxMsg> transactions, int order,
                              byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.headerMsg = headerMsg;
        this.blockHash = blockHash;
        this.transactions = transactions;
        this.order = order;
        init();
    }

    public static PartialBlockTxnBuilder builder() {
        return new PartialBlockTxnBuilder();
    }

    public HeaderMsg getHeaderMsg()         { return this.headerMsg;}
    public HashMsg getBlockHash()           { return blockHash; }
    public List<TxMsg> getTransactions()    { return transactions; }
    public int getOrder()                   { return order; }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return headerMsg.getLengthInBytes()
                + blockHash.calculateLength()
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
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PartialBlockTxnMsg other = (PartialBlockTxnMsg) obj;
        return Objects.equal(this.headerMsg, other.headerMsg)
                && Objects.equal(this.blockHash, other.blockHash)
                && Objects.equal(this.transactions, other.transactions)
                && Objects.equal(this.order, other.order);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), headerMsg, blockHash, transactions, order);
    }

    public PartialBlockTxnBuilder toBuilder() {
        return new PartialBlockTxnBuilder(super.extraBytes, super.checksum)
                        .headerMsg(this.headerMsg)
                        .blockHash(this.blockHash)
                        .transactions(this.transactions)
                        .order(this.order);
    }

    /**
     * Builder
     */
    public static class PartialBlockTxnBuilder extends BodyMessageBuilder {
        private HeaderMsg headerMsg;
        private HashMsg blockHash;
        private List<TxMsg> transactions;
        private int order;

        public PartialBlockTxnBuilder() {}
        public PartialBlockTxnBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public PartialBlockTxnBuilder headerMsg(HeaderMsg headerMsg) {
            this.headerMsg = headerMsg;
            return this;
        }

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
            return new PartialBlockTxnMsg(headerMsg, blockHash, transactions, order, super.extraBytes, super.checksum);
        }
    }
}
