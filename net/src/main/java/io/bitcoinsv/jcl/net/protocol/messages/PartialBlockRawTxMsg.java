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

    private final long txsByteLength;

    public PartialBlockRawTxMsg(BlockHeaderMsg blockHeader, List<RawTxMsg> txs, VarIntMsg txsOrderNumber,
                                byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHeader = blockHeader;
        this.txs = txs;
        this.txsOrderNumber = txsOrderNumber;
        txsByteLength = txs.stream().collect(Collectors.summingLong(t -> t.getLengthInBytes()));
        init();
    }

    public static PartialBlockRawTxMsgBuilder builder() {
        return new PartialBlockRawTxMsgBuilder();
    }

    @Override
    protected long calculateLength() {
        return blockHeader.getLengthInBytes() + txsByteLength + txsOrderNumber.getLengthInBytes();
    }

    @Override
    protected void validateMessage() {
        if (txs == null || txs.size() == 0) throw new RuntimeException("TXs is empty or null");
        if (txsOrderNumber.getValue() < 0) throw new RuntimeException("the txs Order Number must be >= 0");
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public BlockHeaderMsg getBlockHeader()  { return this.blockHeader; }
    public VarIntMsg getTxsOrderNumber()    { return this.txsOrderNumber; }
    public List<RawTxMsg> getTxs()          { return this.txs; }

    @Override
    public String toString() {
        return "PartialBlockRawTxs(blockHeader=" + this.getBlockHeader() + ", txs.length=" + this.txsByteLength + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), blockHeader, txs, txsOrderNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PartialBlockRawTxMsg other = (PartialBlockRawTxMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
                && Objects.equal(this.txs, other.txs)
                && Objects.equal(this.txsOrderNumber, other.txsOrderNumber);
    }

    public PartialBlockRawTxMsgBuilder toBuilder() {
        return new PartialBlockRawTxMsgBuilder(super.extraBytes, super.checksum)
                        .blockHeader(this.blockHeader)
                        .txs(this.txs)
                        .txsOrdersNumber(this.txsOrderNumber);
    }

    /**
     * Builder
     */
    public static class PartialBlockRawTxMsgBuilder extends BodyMessageBuilder {
        private BlockHeaderMsg blockHeader;
        private List<RawTxMsg> txs;
        private VarIntMsg txsOrderNumber;

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

        public PartialBlockRawTxMsg build() {
            return new PartialBlockRawTxMsg(blockHeader, txs, txsOrderNumber, super.extraBytes, super.checksum);
        }
    }
}
