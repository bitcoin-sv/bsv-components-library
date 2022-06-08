package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 */
public final class PartialBlockHeaderMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "PartialBlockHeader";

    /**
     * Specified the Format of the Txs of this block when they are broadcast oin future Events
     */
    public enum BlockTxsFormat {
        DESERIALIZED, RAW
    }

    // Original Header Msg: Included here in case the client of JCL receiving the partial Messages
    // wants to calculate and verify the checksum of the original message:
    private final HeaderMsg headerMsg;
    // Block Header
    private final BlockHeaderMsg blockHeader;
    // Total Size of the Original Block
    private final VarIntMsg txsSizeInBytes;
    // Indicates the format that the Txs of this block will be notified with:
    private final BlockTxsFormat blockTxsFormat;

    public PartialBlockHeaderMsg(HeaderMsg headerMsg, BlockHeaderMsg blockHeader,
                                 VarIntMsg txsSizeInBytes, BlockTxsFormat blockTxsFormat,
                                 byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.headerMsg = headerMsg;
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

    public HeaderMsg getHeaderMsg()             { return this.headerMsg;}
    public BlockHeaderMsg getBlockHeader()      { return this.blockHeader; }
    public VarIntMsg getTxsSizeInbytes()        { return this.txsSizeInBytes;}
    public BlockTxsFormat getBlockTxsFormat()   { return this.blockTxsFormat;}

    @Override
    public String toString() {
        return "PartialBlockHeaderMsg(headerMsg=" + this.headerMsg + ", blockHeader=" + this.getBlockHeader() + ", blockSizeInBytes = " + this.getTxsSizeInbytes() + ", nextTxsInRawFormat)";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), blockHeader);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PartialBlockHeaderMsg other = (PartialBlockHeaderMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader);
    }

    @Override
    public PartialBlockHeaderMsgBuilder toBuilder() {
        return new PartialBlockHeaderMsgBuilder(super.extraBytes, super.checksum)
                .headerMsg(this.headerMsg)
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
    public static class PartialBlockHeaderMsgBuilder extends BodyMessageBuilder {
        private HeaderMsg headerMsg;
        private BlockHeaderMsg blockHeader;
        private VarIntMsg txsSizeInBytes;
        private BlockTxsFormat blockTxsFormat;

        public PartialBlockHeaderMsgBuilder() { }
        public PartialBlockHeaderMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public PartialBlockHeaderMsg.PartialBlockHeaderMsgBuilder headerMsg(HeaderMsg headerMsg) {
            this.headerMsg = headerMsg;
            return this;
        }

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
            return new PartialBlockHeaderMsg(headerMsg, blockHeader, txsSizeInBytes, blockTxsFormat, super.extraBytes, super.checksum);
        }
    }
}
