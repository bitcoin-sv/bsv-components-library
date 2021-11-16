package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * <p>A block is a group of transactions, and is one of the fundamental data structures of the Bitcoin system.
 * It records a set of {@link TxMsg}s together with some data that links it into a place in the global block
 * chain, and proves that a difficult calculation was done over its contents. <p/>
 *
 * This Message is an alternative representation of a block, where the Txs are stored in RAW format, as
 * they are received over the wire.
 *
 * In this case, we use {@link RawMsg} to store the Raw part, which is ONLY the Txs. The Header is stored separately
 * ion this Class.
 */
public final class RawTxBlockMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "Block";
    // Txs in raw format (it might contains partial Txs (broken in the middle):
    private final List<RawTxMsg> txs;

    private final BlockHeaderMsg blockHeader;

    private long txsByteLength;

    // Constructor (specifying the Block Header and All Txs
    protected RawTxBlockMsg(BlockHeaderMsg blockHeader, List<RawTxMsg> txs) {
        this.blockHeader = blockHeader;
        txsByteLength = txs.stream().collect(Collectors.summingLong(t -> t.getLengthInBytes()));
        this.txs = txs;
    }

    public static RawTxBlockMsgBuilder builder() {
        return new RawTxBlockMsgBuilder();
    }

    @Override
    protected long calculateLength() {
        long length = blockHeader.calculateLength() + txsByteLength;
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

    public List<RawTxMsg> getTxs() {
        return txs;
    }

    public String toString() {
        return "RawBlockMsg(blockHeader=" + this.getBlockHeader() + ", txs.length=" + txsByteLength + ")";
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
        RawTxBlockMsg other = (RawTxBlockMsg) obj;
        return Objects.equal(this.blockHeader, other.blockHeader)
                && Objects.equal(this.txs, other.txs);
    }

    /**
     * Builder
     */
    public static class RawTxBlockMsgBuilder {
        private BlockHeaderMsg blockHeader;
        private List<RawTxMsg> txs;

        RawTxBlockMsgBuilder() {
        }

        public RawTxBlockMsgBuilder blockHeader(BlockHeaderMsg blockHeader) {
            this.blockHeader = blockHeader;
            return this;
        }

        public RawTxBlockMsgBuilder txs(List<RawTxMsg> txs) {
            this.txs = txs;
            return this;
        }

        public RawTxBlockMsg build() {
            return new RawTxBlockMsg(blockHeader, txs);
        }

    }
}
