package io.bitcoinsv.bsvcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class BlockTxnMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "blocktxn";

    private final HashMsg blockHash;
    private final List<TxMsg> transactions;

    public BlockTxnMsg(HashMsg blockHash, List<TxMsg> transactions,
                       byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHash = blockHash;
        this.transactions = transactions;
        init();
    }

    public static BlockTxnMsgBuilder builder() {
        return new BlockTxnMsgBuilder();
    }

    @Override
    public String getMessageType()          { return MESSAGE_TYPE; }
    public HashMsg getBlockHash()           { return blockHash; }
    public List<TxMsg> getTransactions()    { return transactions; }

    @Override
    protected long calculateLength() {
        return blockHash.calculateLength()
            + VarIntMsg.builder().value(transactions.size()).build().calculateLength()
            + transactions.stream()
            .mapToLong(TxMsg::calculateLength)
            .sum();
    }

    @Override
    protected void validateMessage() {}

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BlockTxnMsg other = (BlockTxnMsg) obj;
        return Objects.equal(this.blockHash, other.blockHash)
                && Objects.equal(this.transactions, other.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.blockHash, this.transactions);
    }

    @Override
    public BlockTxnMsgBuilder toBuilder() {
        return new BlockTxnMsgBuilder(super.extraBytes, super.checksum)
                    .blockHash(this.blockHash)
                    .transactions(this.transactions);
    }

    /**
     * Builder
     */
    public static class BlockTxnMsgBuilder extends BodyMessageBuilder {
        private HashMsg blockHash;
        private List<TxMsg> transactions;

        public BlockTxnMsgBuilder() {}
        public BlockTxnMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public BlockTxnMsgBuilder blockHash(HashMsg blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public BlockTxnMsgBuilder transactions(List<TxMsg> transactions) {
            this.transactions = transactions;
            return this;
        }

        public BlockTxnMsg build() {
            return new BlockTxnMsg(blockHash, transactions, super.extraBytes, super.checksum);
        }
    }
}
