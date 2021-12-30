package io.bitcoinsv.jcl.net.protocol.messages;

import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class GetBlockTxnMsg extends BodyMessage implements Serializable {
    public static final String MESSAGE_TYPE = "getblocktxn";

    private final HashMsg blockHash;
    private final VarIntMsg indexesLength;
    private final List<VarIntMsg> indexes;

    public GetBlockTxnMsg(HashMsg blockHash, VarIntMsg indexesLength, List<VarIntMsg> indexes,
                          byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHash = blockHash;
        this.indexesLength = indexesLength;
        this.indexes = indexes;
        init();
    }

    public HashMsg getBlockHash()       { return blockHash; }
    public VarIntMsg getIndexesLength() { return indexesLength; }
    public List<VarIntMsg> getIndexes() { return indexes; }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return blockHash.calculateLength()
            + indexesLength.calculateLength()
            + indexes.stream()
            .mapToLong(VarIntMsg::calculateLength)
            .sum();
    }

    @Override
    protected void validateMessage() {
    }

    @Override
    public BlockTransactionsRequestMsgBuilder toBuilder() {
        return new BlockTransactionsRequestMsgBuilder(super.extraBytes, super.checksum)
                    .blockHash(this.blockHash)
                    .indexesLength(this.indexesLength)
                    .indexes(this.indexes);
    }

    public static BlockTransactionsRequestMsgBuilder builder() {
        return new BlockTransactionsRequestMsgBuilder();
    }

    /**
     * Builder
     */
    public static class BlockTransactionsRequestMsgBuilder extends BodyMessageBuilder {
        private HashMsg blockHash;
        private VarIntMsg indexesLength;
        private List<VarIntMsg> indexes;

        public BlockTransactionsRequestMsgBuilder() {}
        public BlockTransactionsRequestMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}
        public BlockTransactionsRequestMsgBuilder blockHash(HashMsg blockHash) {
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

        public GetBlockTxnMsg build() {
            return new GetBlockTxnMsg(blockHash, indexesLength, indexes, super.extraBytes, super.checksum);
        }
    }
}