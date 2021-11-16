package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class PartialBlockRawTXsMsg extends Message {

    public static final String MESSAGE_TYPE = "PartialBlockRawTxs";
    private final BlockHeaderMsg blockHeader;
    // Txs in raw format (it might contains partial Txs (broken in the middle):
    private final byte[] txs;
    // This field stores the order of this Batch of Txs within the Block (zero-based)
    private final VarIntMsg txsOrderNumber;

    public PartialBlockRawTXsMsg(BlockHeaderMsg blockHeader, byte[] txs, VarIntMsg txsOrderNumber) {
        this.blockHeader = blockHeader;
        this.txs = txs;
        this.txsOrderNumber = txsOrderNumber;
        init();
    }

    public static PartialBlockTXsMsgBuilder builder() {
        return new PartialBlockTXsMsgBuilder();
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

    @Override
    public String getMessageType()          { return MESSAGE_TYPE; }
    public BlockHeaderMsg getBlockHeader()  { return this.blockHeader; }
    public VarIntMsg getTxsOrderNumber()    { return this.txsOrderNumber; }
    public byte[] getTxs()                  { return this.txs; }

    @Override
    public String toString() {
        return "PartialBlockRawTxs(blockHeader=" + this.getBlockHeader() + ", txs.length=" + this.getTxs().length + ")";
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
        PartialBlockRawTXsMsg other = (PartialBlockRawTXsMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
            && Objects.equal(this.txs, other.txs)
            && Objects.equal(this.txsOrderNumber, other.txsOrderNumber);
    }

    @Override
    public PartialBlockTXsMsgBuilder toBuilder() {
        return new PartialBlockTXsMsgBuilder()
                        .blockHeader(this.blockHeader)
                        .txs(this.txs)
                        .txsOrdersNumber(this.txsOrderNumber);
    }

    /**
     * Builder
     */
    public static class PartialBlockTXsMsgBuilder extends MessageBuilder{
        private BlockHeaderMsg blockHeader;
        private byte[] txs;
        private VarIntMsg txsOrderNumber;

        PartialBlockTXsMsgBuilder() {
        }

        public PartialBlockRawTXsMsg.PartialBlockTXsMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockRawTXsMsg.PartialBlockTXsMsgBuilder txs(byte[] txs) {
            this.txs = txs;
            return this;
        }

        public PartialBlockRawTXsMsg.PartialBlockTXsMsgBuilder txsOrdersNumber(long orderNumber) {
            this.txsOrderNumber = VarIntMsg.builder().value(orderNumber).build();
            return this;
        }

        public PartialBlockRawTXsMsg.PartialBlockTXsMsgBuilder txsOrdersNumber(VarIntMsg orderNumber) {
            this.txsOrderNumber = orderNumber;
            return this;
        }

        public PartialBlockRawTXsMsg build() {
            return new PartialBlockRawTXsMsg(blockHeader, txs, txsOrderNumber);
        }
    }
}
