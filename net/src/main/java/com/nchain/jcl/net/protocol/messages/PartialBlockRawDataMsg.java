package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class PartialBlockRawDataMsg extends Message {

    public static final String MESSAGE_TYPE = "PartialBlockRawDataMsg";
    private final BlockHeaderMsg blockHeader;
    // Txs in raw format (it might contains partial Txs (broken in the middle):
    private final byte[] txs;
    // This field stores the order of this Batch of Txs within the Block (zero-based)
    private final VarIntMsg txsOrderNumber;

    public PartialBlockRawDataMsg(BlockHeaderMsg blockHeader, byte[] txs, VarIntMsg txsOrderNumber) {
        this.blockHeader = blockHeader;
        this.txs = txs;
        this.txsOrderNumber = txsOrderNumber;
        init();
    }

    public static PartialBlockRawDataMsgBuilder builder() {
        return new PartialBlockRawDataMsgBuilder();
    }

    @Override
    protected long calculateLength() {
        return blockHeader.getLengthInBytes() + txs.length + txsOrderNumber.getLengthInBytes();
    }

    @Override
    protected void validateMessage() {
        if (txs == null || txs.length == 0) throw new RuntimeException("TXs bytes is empty or null");
        if (txsOrderNumber.getValue() < 0) throw new RuntimeException("the txs Order Number must be >= 0");
    }

    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public BlockHeaderMsg getBlockHeader() {
        return this.blockHeader;
    }

    public VarIntMsg getTxsOrderNumber() {
        return this.txsOrderNumber;
    }

    public byte[] getTxs() {
        return this.txs;
    }

    @Override
    public String toString() {
        return "PartialBlockRawDataMsg(blockHeader=" + this.getBlockHeader() + ", txs.length=" + this.getTxs().length + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(blockHeader, txs);
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
        PartialBlockRawDataMsg other = (PartialBlockRawDataMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
            && Objects.equal(this.txs, other.txs)
            && Objects.equal(this.txsOrderNumber, other.txsOrderNumber);
    }

    /**
     * Builder
     */
    public static class PartialBlockRawDataMsgBuilder {
        private BlockHeaderMsg blockHeader;
        private byte[] txs;
        private VarIntMsg txsOrderNumber;

        PartialBlockRawDataMsgBuilder() {
        }

        public PartialBlockRawDataMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockRawDataMsgBuilder txs(byte[] txs) {
            this.txs = txs;
            return this;
        }

        public PartialBlockRawDataMsgBuilder txsOrdersNumber(long orderNumber) {
            this.txsOrderNumber = VarIntMsg.builder().value(orderNumber).build();
            return this;
        }

        public PartialBlockRawDataMsg build() {
            return new PartialBlockRawDataMsg(blockHeader, txs, txsOrderNumber);
        }
    }
}
