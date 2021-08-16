/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author n.srivastava@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * <p>A block is a group of transactions, and is one of the fundamental data structures of the Bitcoin system.
 * * It records a set of {@link TxMsg}s together with some data that links it into a place in the global block
 * * chain, and proves that a difficult calculation was done over its contents. <p/>
 * *
 */
public final class BlockMsg extends Message {

    public static final String MESSAGE_TYPE = "Block";

    private final BlockHeaderMsg blockHeader;
    private final List<TxMsg> transactionMsg;

    // Constructor (specifying the Block Header and All Txs
    protected BlockMsg(BlockHeaderMsg blockHeader, List<TxMsg> transactionMsgs) {
        this.blockHeader = blockHeader;
        this.transactionMsg = transactionMsgs;
        init();
    }

    public static BlockMsgBuilder builder() {
        return new BlockMsgBuilder();
    }

    @Override
    protected long calculateLength() {
        long length = blockHeader.calculateLength();
        for (TxMsg transaction : transactionMsg)
            length += transaction.getLengthInBytes();
        return length;
    }

    @Override
    protected void validateMessage() {
        checkState(blockHeader.getTransactionCount().getValue() == transactionMsg.size(),
            "The number of Txs must match the field in the 'txn_count'");
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public BlockHeaderMsg getBlockHeader() {
        return this.blockHeader;
    }

    public List<TxMsg> getTransactionMsg() {
        return this.transactionMsg;
    }

    public String toString() {
        return "BlockMsg(blockHeader=" + this.getBlockHeader() + ", transactionMsg=" + this.getTransactionMsg() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(blockHeader, transactionMsg);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        BlockMsg other = (BlockMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
            && Objects.equal(this.transactionMsg, other.transactionMsg);
    }

    /**
     * Builder
     */
    public static class BlockMsgBuilder {
        private BlockHeaderMsg blockHeader;
        private List<TxMsg> transactionMsgs;

        BlockMsgBuilder() {
        }

        public BlockMsg.BlockMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public BlockMsg.BlockMsgBuilder transactionMsgs(List<TxMsg> transactionMsgs) {
            this.transactionMsgs = transactionMsgs;
            return this;
        }

        public BlockMsg build() {
            return new BlockMsg(blockHeader, transactionMsgs);
        }

    }
}
