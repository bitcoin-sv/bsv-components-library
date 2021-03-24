package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class PartialBlockTXsMsg extends Message {

    public static final String MESSAGE_TYPE = "PartialBlockTxs";
    private final CompleteBlockHeaderMsg blockHeader;
    private final List<TxMsg> txs;

    public PartialBlockTXsMsg(CompleteBlockHeaderMsg blockHeader, List<TxMsg> txs) {
        this.blockHeader = blockHeader;
        this.txs = txs;
        init();
    }

    @Override
    protected long calculateLength() {
         return blockHeader.getLengthInBytes() + txs.stream().mapToLong(tx -> tx.getLengthInBytes()).sum();
    }

    @Override
    protected void validateMessage() {
        if (txs == null || txs.size() == 0) throw new RuntimeException("The List of TXs is empty or null");
    }

    @Override
    public String getMessageType()  { return MESSAGE_TYPE; }
    public CompleteBlockHeaderMsg getBlockHeader()  { return this.blockHeader; }
    public List<TxMsg> getTxs()             { return this.txs; }

    @Override
    public String toString() {
        return "PartialBlockTXsMsg(blockHeader=" + this.getBlockHeader() + ", txs=" + this.getTxs() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(blockHeader, txs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        PartialBlockTXsMsg other = (PartialBlockTXsMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
                && Objects.equal(this.txs, other.txs);
    }

    public static PartialBlockTXsMsgBuilder builder() {
        return new PartialBlockTXsMsgBuilder();
    }

    /**
     * Builder
     */
    public static class PartialBlockTXsMsgBuilder {
        private CompleteBlockHeaderMsg blockHeader;
        private List<TxMsg> txs;

        PartialBlockTXsMsgBuilder() {}

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder blockHeader(CompleteBlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder txs(List<TxMsg> txs) {
            this.txs = txs;
            return this;
        }

        public PartialBlockTXsMsg build() {
            return new PartialBlockTXsMsg(blockHeader, txs);
        }
    }
}
