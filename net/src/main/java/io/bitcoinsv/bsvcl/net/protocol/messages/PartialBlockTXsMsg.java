package io.bitcoinsv.bsvcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class PartialBlockTXsMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "PartialBlockTxs";
    private final BlockHeaderMsg blockHeader;
    private final List<TxMsg> txs;
    // This field stores the order of this Batch of Txs within the Block (zero-based)
    private final VarIntMsg txsOrderNumber;
    // It stores the index of the first Tx in this chunk within the Whole Block
    private final VarIntMsg txsIndexNumber;

    public PartialBlockTXsMsg(BlockHeaderMsg blockHeader,
                              List<TxMsg> txs,
                              VarIntMsg txsOrderNumber,
                              VarIntMsg txsIndexNumber,
                              byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHeader = blockHeader;
        this.txs = txs;
        this.txsOrderNumber = txsOrderNumber;
        this.txsIndexNumber = txsIndexNumber;
        init();
    }

    public static PartialBlockTXsMsgBuilder builder() {
        return new PartialBlockTXsMsgBuilder();
    }

    @Override
    protected long calculateLength() {
        return blockHeader.getLengthInBytes() + txs.stream().mapToLong(tx -> tx.getLengthInBytes()).sum() + txsOrderNumber.getLengthInBytes() + txsIndexNumber.getLengthInBytes();
    }

    @Override
    protected void validateMessage() {
        if (txs == null || txs.size() == 0) throw new RuntimeException("The List of TXs is empty or null");
        if (txsOrderNumber.getValue() < 0) throw new RuntimeException("the txs Order Number must be >= 0");
        if (txsIndexNumber.getValue() < 0) throw new RuntimeException("the txs Index Number must be >= 0");
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

    public VarIntMsg getTxsIndexNumber() { return this.txsIndexNumber;}

    public List<TxMsg> getTxs() {
        return this.txs;
    }

    @Override
    public String toString() {
        return "PartialBlockTXsMsg(blockHeader=" + this.getBlockHeader() + ", txs=" + this.getTxs() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), blockHeader, txs, txsOrderNumber, txsIndexNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PartialBlockTXsMsg other = (PartialBlockTXsMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
                && Objects.equal(this.txs, other.txs)
                && Objects.equal(this.txsOrderNumber, other.txsOrderNumber)
                && Objects.equal(this.txsIndexNumber, other.txsIndexNumber);
    }

    public PartialBlockTXsMsgBuilder toBuilder() {
        return new PartialBlockTXsMsgBuilder(super.extraBytes, super.checksum)
                        .blockHeader(this.blockHeader)
                        .txs(this.txs)
                        .txsOrdersNumber(this.txsOrderNumber)
                        .txsIndexNumber(this.txsIndexNumber);
    }

    /**
     * Builder
     */
    public static class PartialBlockTXsMsgBuilder extends BodyMessageBuilder{
        private BlockHeaderMsg blockHeader;
        private List<TxMsg> txs;
        private VarIntMsg txsOrderNumber;
        private VarIntMsg txsIndexNumber;

        public PartialBlockTXsMsgBuilder() { }
        public PartialBlockTXsMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder txs(List<TxMsg> txs) {
            this.txs = txs;
            return this;
        }

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder txsOrdersNumber(long orderNumber) {
            this.txsOrderNumber = VarIntMsg.builder().value(orderNumber).build();
            return this;
        }

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder txsOrdersNumber(VarIntMsg orderNumber) {
            this.txsOrderNumber = orderNumber;
            return this;
        }

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder  txsIndexNumber(long indexNumber) {
            this.txsIndexNumber = VarIntMsg.builder().value(indexNumber).build();
            return this;
        }

        public PartialBlockTXsMsg.PartialBlockTXsMsgBuilder  txsIndexNumber(VarIntMsg indexNumber) {
            this.txsIndexNumber = indexNumber;
            return this;
        }

        public PartialBlockTXsMsg build() {
            return new PartialBlockTXsMsg(blockHeader, txs, txsOrderNumber, txsIndexNumber, super.extraBytes, super.checksum);
        }
    }
}
