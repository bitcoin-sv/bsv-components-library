package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 */
public final class PartialBlockHeaderMsg extends Message {
    public static final String MESSAGE_TYPE = "PartialBlockHeader";
    private final BlockHeaderMsg blockHeader;   // Block Header
    private final VarIntMsg blockSizeInBytes;   // Total Size of the Original Block

    public PartialBlockHeaderMsg(BlockHeaderMsg blockHeader, VarIntMsg blockSizeInBytes) {
        this.blockHeader = blockHeader;
        this.blockSizeInBytes = blockSizeInBytes;
        init();
    }

    @Override
    protected long calculateLength() {
        return blockHeader.calculateLength() + blockSizeInBytes.calculateLength();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void validateMessage() {}

    public BlockHeaderMsg getBlockHeader() {
        return this.blockHeader;
    }
    public VarIntMsg getBlockSizeInbytes() { return this.blockSizeInBytes;}

    @Override
    public String toString() {
        return "PartialBlockHeaderMsg(blockHeader=" + this.getBlockHeader() + ", blockSizeInBytes = " + this.getBlockSizeInbytes() + ")";
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

    public static PartialBlockHeaderMsgBuilder builder() {
        return new PartialBlockHeaderMsgBuilder();
    }

    /**
     * Builder
     */
    public static class PartialBlockHeaderMsgBuilder {
        private BlockHeaderMsg blockHeader;
        private VarIntMsg blockSizeInBytes;

        PartialBlockHeaderMsgBuilder() { }

        public PartialBlockHeaderMsg.PartialBlockHeaderMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockHeaderMsg.PartialBlockHeaderMsgBuilder blockSizeInBytes(Long blockSizeInBytes) {
            if (blockSizeInBytes != null) {
                this.blockSizeInBytes = VarIntMsg.builder().value(blockSizeInBytes).build();
            }
            return this;
        }

        public PartialBlockHeaderMsg build() {
            return new PartialBlockHeaderMsg(blockHeader, blockSizeInBytes);
        }
    }
}
