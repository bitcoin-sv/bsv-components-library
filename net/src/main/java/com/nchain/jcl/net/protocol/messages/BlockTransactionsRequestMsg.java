package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;

import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class BlockTransactionsRequestMsg extends Message {
    public static final String MESSAGE_TYPE = "getblocktxn";

    private final byte[] blockHash;
    private final VarIntMsg indexesLength;
    private final List<VarIntMsg> indexes;

    public BlockTransactionsRequestMsg(byte[] blockHash, VarIntMsg indexesLength, List<VarIntMsg> indexes) {
        this.blockHash = blockHash;
        this.indexesLength = indexesLength;
        this.indexes = indexes;
    }

    public static BlockTransactionsRequestMsgBuilder builder() {
        return new BlockTransactionsRequestMsgBuilder();
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public VarIntMsg getIndexesLength() {
        return indexesLength;
    }

    public List<VarIntMsg> getIndexes() {
        return indexes;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return blockHash.length
            + indexesLength.calculateLength()
            + indexes.stream()
            .mapToLong(VarIntMsg::calculateLength)
            .sum();
    }

    @Override
    protected void validateMessage() {

    }

    public static class BlockTransactionsRequestMsgBuilder {
        private byte[] blockHash;
        private VarIntMsg indexesLength;
        private List<VarIntMsg> indexes;

        public BlockTransactionsRequestMsgBuilder blockHash(byte[] blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public BlockTransactionsRequestMsgBuilder indexesLength(VarIntMsg indexesLength) {
            this.indexesLength = indexesLength;
            return this;
        }

        public BlockTransactionsRequestMsgBuilder indexes(List<VarIntMsg> indexes) {
            this.indexes = indexes;
            return this;
        }

        public BlockTransactionsRequestMsg build() {
            return new BlockTransactionsRequestMsg(blockHash, indexesLength, indexes);
        }
    }
}
