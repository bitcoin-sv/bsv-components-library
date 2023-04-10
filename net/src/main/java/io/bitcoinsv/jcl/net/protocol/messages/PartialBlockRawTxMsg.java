package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class PartialBlockRawTxMsg extends BodyMessage {

    public static final String MESSAGE_TYPE = "PartialBlockRawTxMsg";
    private final BlockHeaderMsg blockHeader;
    // Txs in raw format (it might contains partial Txs (broken in the middle):
    private final List<RawTxMsg> txs;
    // This field stores the order of this Batch of Txs within the Block (zero-based)
    private final VarIntMsg txsOrderNumber;
    // It stores the index of the first Tx in this chunk within the Whole Block
    private final VarIntMsg txsIndexNumber;

    private final long txsByteLength;

    public PartialBlockRawTxMsg(BlockHeaderMsg blockHeader,
                                List<RawTxMsg> txs,
                                VarIntMsg txsOrderNumber,
                                VarIntMsg txsIndexNumber,
                                byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHeader = blockHeader;
        this.txs = txs;
        this.txsOrderNumber = txsOrderNumber;
        this.txsIndexNumber = txsIndexNumber;
        txsByteLength = txs.stream().collect(Collectors.summingLong(t -> t.getLengthInBytes()));
        init();
    }

    public static PartialBlockRawTxMsgBuilder builder() {
        return new PartialBlockRawTxMsgBuilder();
    }

    @Override
    protected long calculateLength() {
        return blockHeader.getLengthInBytes() + txsByteLength + txsOrderNumber.getLengthInBytes() + txsIndexNumber.getLengthInBytes();
    }

    @Override
    protected void validateMessage() {
        if (txs == null || txs.size() == 0) throw new RuntimeException("TXs is empty or null");
        if (txsOrderNumber.getValue() < 0) throw new RuntimeException("the txs Order Number must be >= 0");
        if (txsIndexNumber.getValue() < 0) throw new RuntimeException("the txs Index Number must be >= 0");
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public BlockHeaderMsg getBlockHeader()  { return this.blockHeader; }
    public VarIntMsg getTxsOrderNumber()    { return this.txsOrderNumber; }
    public VarIntMsg getTxsIndexNumber()    { return this.txsIndexNumber;}
    public List<RawTxMsg> getTxs()          { return this.txs; }

    @Override
    public String toString() {
        return "PartialBlockRawTxs(blockHeader=" + this.getBlockHeader() + ", txs.length=" + this.txsByteLength + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), blockHeader, txs, txsOrderNumber, txsIndexNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PartialBlockRawTxMsg other = (PartialBlockRawTxMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
                && Objects.equal(this.txs, other.txs)
                && Objects.equal(this.txsOrderNumber, other.txsOrderNumber)
                && Objects.equal(this.txsIndexNumber, other.txsIndexNumber);
    }

    public PartialBlockRawTxMsgBuilder toBuilder() {
        return new PartialBlockRawTxMsgBuilder(super.extraBytes, super.checksum)
                        .blockHeader(this.blockHeader)
                        .txs(this.txs)
                        .txsOrdersNumber(this.txsOrderNumber)
                        .txsIndexNumber(this.txsIndexNumber);
    }

    /**
     * Builder
     */
    public static class PartialBlockRawTxMsgBuilder extends BodyMessageBuilder {
        private BlockHeaderMsg blockHeader;
        private List<RawTxMsg> txs;
        private VarIntMsg txsOrderNumber;
        private VarIntMsg txsIndexNumber;

        public PartialBlockRawTxMsgBuilder() {}
        public PartialBlockRawTxMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public PartialBlockRawTxMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockRawTxMsgBuilder txs(List<RawTxMsg> txs) {
            this.txs = txs;
            return this;
        }

        public PartialBlockRawTxMsgBuilder txsOrdersNumber(long orderNumber) {
            this.txsOrderNumber = VarIntMsg.builder().value(orderNumber).build();
            return this;
        }

        public PartialBlockRawTxMsgBuilder txsOrdersNumber(VarIntMsg orderNumber) {
            this.txsOrderNumber = orderNumber;
            return this;
        }

        public PartialBlockRawTxMsgBuilder txsIndexNumber(long indexNumber) {
            this.txsIndexNumber = VarIntMsg.builder().value(indexNumber).build();
            return this;
        }

        public PartialBlockRawTxMsgBuilder txsIndexNumber(VarIntMsg indexNumber) {
            this.txsIndexNumber = indexNumber;
            return this;
        }

        public PartialBlockRawTxMsg build() {
            return new PartialBlockRawTxMsg(blockHeader, txs, txsOrderNumber, txsIndexNumber, super.extraBytes, super.checksum);
        }
    }
}
