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
    private final BlockHeaderMsg blockHeader;

    public PartialBlockHeaderMsg(BlockHeaderMsg blockHeader) {
        this.blockHeader = blockHeader;
        init();
    }

    @Override
    protected long calculateLength() { return blockHeader.calculateLength(); }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void validateMessage() {}

    public BlockHeaderMsg getBlockHeader() {
        return this.blockHeader;
    }

    @Override
    public String toString() {
        return "PartialBlockHeaderMsg(blockHeader=" + this.getBlockHeader() + ")";
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

        PartialBlockHeaderMsgBuilder() { }

        public PartialBlockHeaderMsg.PartialBlockHeaderMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public PartialBlockHeaderMsg build() {
            return new PartialBlockHeaderMsg(blockHeader);
        }
    }
}
