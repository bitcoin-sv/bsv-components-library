package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public final class BasicBlockHeaderMsg extends BlockHeaderMsg {

    public static final String MESSAGE_TYPE = "BasicBlockHeader";

    public BasicBlockHeaderMsg(HashMsg hash, long version, HashMsg prevBlockHash, HashMsg merkleRoot,
                               long creationTimestamp, long difficultyTarget, long nonce,
                               long transactionCount) {
        super(
            hash,
            version,
            prevBlockHash,
            merkleRoot,
            creationTimestamp,
            difficultyTarget,
            nonce,
            VarIntMsg.builder().value(transactionCount).build()
        );
    }

    public static BlockHeaderMsgBuilder builder() {
        return new BlockHeaderMsgBuilder();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    public String toString() {
        return "BasicBlockHeaderMsg(hash=" + this.getHash() + ", version=" + this.getVersion() + ", prevBlockHash=" + this.getPrevBlockHash() + ", merkleRoot=" + this.getMerkleRoot() + ", creationTimestamp=" + this.getCreationTimestamp() + ", difficultyTarget=" + this.getDifficultyTarget() + ", nonce=" + this.getNonce() + ", transactionCount=" + this.getTransactionCount() + ")";
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
        BasicBlockHeaderMsg other = (BasicBlockHeaderMsg) obj;

        return Objects.equal(this.hash, other.hash)
            && Objects.equal(this.version, other.version)
            && Objects.equal(this.prevBlockHash, other.prevBlockHash)
            && Objects.equal(this.merkleRoot, other.merkleRoot)
            && Objects.equal(this.creationTimestamp, other.creationTimestamp)
            && Objects.equal(this.difficultyTarget, other.difficultyTarget)
            && Objects.equal(this.nonce, other.nonce)
            && Objects.equal(this.transactionCount, other.transactionCount);
    }

    /**
     * Builder
     */
    public static class BlockHeaderMsgBuilder {
        private HashMsg hash;
        private long version;
        private HashMsg prevBlockHash;
        private HashMsg merkleRoot;
        private long creationTimestamp;
        private long difficultyTarget;
        private long nonce;

        BlockHeaderMsgBuilder() {
        }

        public BasicBlockHeaderMsg.BlockHeaderMsgBuilder hash(HashMsg hash) {
            this.hash = hash;
            return this;
        }

        public BasicBlockHeaderMsg.BlockHeaderMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public BasicBlockHeaderMsg.BlockHeaderMsgBuilder prevBlockHash(HashMsg prevBlockHash) {
            this.prevBlockHash = prevBlockHash;
            return this;
        }

        public BasicBlockHeaderMsg.BlockHeaderMsgBuilder merkleRoot(HashMsg merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public BasicBlockHeaderMsg.BlockHeaderMsgBuilder creationTimestamp(long creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public BasicBlockHeaderMsg.BlockHeaderMsgBuilder difficultyTarget(long difficultyTarget) {
            this.difficultyTarget = difficultyTarget;
            return this;
        }

        public BasicBlockHeaderMsg.BlockHeaderMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public BasicBlockHeaderMsg build() {
            return new BasicBlockHeaderMsg(hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce, 0);
        }
    }
}
