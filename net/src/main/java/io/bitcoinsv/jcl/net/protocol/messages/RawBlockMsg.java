package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * <p>A block is a group of transactions, and is one of the fundamental data structures of the Bitcoin system.
 * It records a set of {@link TxMsg}s together with some data that links it into a place in the global block
 * chain, and proves that a difficult calculation was done over its contents. <p/>
 *
 * This Message is an alternative representation of a block, where the Txs are stored in RAW format (byte array), as
 * they are received over the wire.
 *
 * In this case, we use {@link RawMsg} to store the Raw part, which is ONLY the Txs. The Header is stored separately
 * ion this Class.
 */
public final class RawBlockMsg extends RawMsg {

    public static final String MESSAGE_TYPE = "Block";
    private final BlockHeaderMsg blockHeader;

    // Constructor (specifying the Block Header and All Txs
    protected RawBlockMsg(BlockHeaderMsg blockHeader, byte[] txs) {
        super(txs);
        this.blockHeader = blockHeader;
        init();
    }

    public static BlockMsgBuilder builder() {
        return new BlockMsgBuilder();
    }

    @Override
    protected long calculateLength() {
        long length = blockHeader.calculateLength() + super.content.length;
        return length;
    }

    @Override
    protected void validateMessage() {
       // No validation
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public BlockHeaderMsg getBlockHeader() {
        return this.blockHeader;
    }

    public byte[] getTxs() {
        return super.content;
    }

    public String toString() {
        return "RawBlockMsg(blockHeader=" + this.getBlockHeader() + ", txs.length=" + this.getTxs().length + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(blockHeader, super.content);
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
        RawBlockMsg other = (RawBlockMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
            && Objects.equal(super.content, super.content);
    }

    /**
     * Builder
     */
    public static class BlockMsgBuilder {
        private BlockHeaderMsg blockHeader;
        private byte[] txs;

        BlockMsgBuilder() {
        }

        public RawBlockMsg.BlockMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public RawBlockMsg.BlockMsgBuilder txs(byte[] txs) {
            this.txs = txs;
            return this;
        }

        public RawBlockMsg build() {
            return new RawBlockMsg(blockHeader, txs);
        }

    }
}
