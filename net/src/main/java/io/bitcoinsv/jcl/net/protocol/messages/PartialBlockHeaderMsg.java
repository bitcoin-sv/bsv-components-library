package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 */
public final class PartialBlockHeaderMsg extends Message {
    public static final String MESSAGE_TYPE = "PartialBlockHeader";

    /**
     * Specified the Format of the Txs of this block when they are broadcast oin future Events
     */
    public enum BlockTxsFormat {
        DESERIALIZED, RAW
    }

    // Block Header
    private final BlockHeaderMsg blockHeader;
    // Total Size of the Original Block
    private final VarIntMsg txsSizeInBytes;
    // Indicates the format that the Txs of this block will be notified with:
    private final BlockTxsFormat blockTxsFormat;

    public PartialBlockHeaderMsg(BlockHeaderMsg blockHeader, VarIntMsg txsSizeInBytes, BlockTxsFormat blockTxsFormat) {
        this.blockHeader = blockHeader;
        this.txsSizeInBytes = txsSizeInBytes;
        this.blockTxsFormat = blockTxsFormat;
        init();
    }

    @Override
    protected long calculateLength() {
        return blockHeader.calculateLength() + txsSizeInBytes.calculateLength();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void validateMessage() {}

    public BlockHeaderMsg getBlockHeader()      { return this.blockHeader; }
    public VarIntMsg getTxsSizeInbytes()        { return this.txsSizeInBytes;}
    public BlockTxsFormat getBlockTxsFormat()   { return this.blockTxsFormat;}

    @Override
    public String toString() {
        return "PartialBlockHeaderMsg(blockHeader=" + this.getBlockHeader() + ", blockSizeInBytes = " + this.getTxsSizeInbytes() + ", nextTxsInRawFormat)";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(blockHeader);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        PartialBlockHeaderMsg other = (PartialBlockHeaderMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader);
    }

    public PartialBlockHeaderMsgBuilder toBuilder() {
        return new PartialBlockHeaderMsgBuilder()
                .blockHeader(this.blockHeader)
                .txsSizeInBytes(this.txsSizeInBytes.getValue())
                .blockTxsFormat(this.blockTxsFormat);
    }

    public static PartialBlockHeaderMsgBuilder builder() {
        return new PartialBlockHeaderMsgBuilder();
    }

    /**
     * Builder
     */
    public static class PartialBlockHeaderMsgBuilder {
        private BlockHeaderMsg blockHeader;
        private VarIntMsg txsSizeInBytes;
        private BlockTxsFormat blockTxsFormat;

        PartialBlockHeaderMsgBuilder() { }

        public PartialBlockHeaderMsg.PartialBlockHeaderMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockHeaderMsg.PartialBlockHeaderMsgBuilder txsSizeInBytes(Long txsSizeInBytes) {
            if (txsSizeInBytes != null) {
                this.txsSizeInBytes = VarIntMsg.builder().value(txsSizeInBytes).build();
            }
            return this;
        }

        public PartialBlockHeaderMsg.PartialBlockHeaderMsgBuilder blockTxsFormat(BlockTxsFormat blockTxsFormat) {
            this.blockTxsFormat = blockTxsFormat;
            return this;
        }

        public PartialBlockHeaderMsg build() {
            return new PartialBlockHeaderMsg(blockHeader, txsSizeInBytes, blockTxsFormat);
        }
    }
}
