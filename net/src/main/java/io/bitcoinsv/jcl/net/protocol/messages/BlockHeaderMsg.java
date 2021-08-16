/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinj.bitcoin.api.base.AbstractBlock;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.bitcoin.bean.base.HeaderBean;
import io.bitcoinj.core.Sha256Hash;

import static java.util.Optional.ofNullable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * The Block Header represents the Header of information wihtint a Block.
 * It's also used in the HEADERS and GETHEADERS packages.
 */
public final class BlockHeaderMsg extends Message {

    public static final String MESSAGE_TYPE = "BlockHeader";

    public static final int TIMESTAMP_LENGTH = 4;

    public static final int NONCE_LENGTH = 4;

    // IMPORTANT: This field (hash) is NOT SERIALIZED.
    // The hash of the block is NOT part of the BLOCK Message itself: its external to it.
    // In order to calculate a Block Hash we need to serialize the Block first, so instead of doing
    // that avery time we need a Hash, we store the Hash here, at the moment when we deserialize the
    // Block for the first time, so its available for further use.
    protected final HashMsg hash;

    protected final long version;
    protected final HashMsg prevBlockHash;
    protected final HashMsg merkleRoot;
    protected final long creationTimestamp;
    protected final long difficultyTarget;
    protected final long nonce;
    private final VarIntMsg transactionCount;

    public BlockHeaderMsg(HashMsg hash, long version, HashMsg prevBlockHash, HashMsg merkleRoot, long creationTimestamp, long difficultyTarget, long nonce, VarIntMsg transactionCount) {
        this.hash = hash;
        this.version = version;
        this.prevBlockHash = prevBlockHash;
        this.merkleRoot = merkleRoot;
        this.creationTimestamp = creationTimestamp;
        this.difficultyTarget = difficultyTarget;
        this.nonce = nonce;
        this.transactionCount = ofNullable(transactionCount).orElse(VarIntMsg.builder().value(0).build());
        init();
    }

    public static BlockHeaderMsgBuilder builder() {
        return new BlockHeaderMsgBuilder();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return 4 + prevBlockHash.getLengthInBytes() + merkleRoot.getLengthInBytes() +
            TIMESTAMP_LENGTH + TIMESTAMP_LENGTH + NONCE_LENGTH + transactionCount.calculateLength();
    }

    @Override
    protected void validateMessage() {
    }

    /**
     * Returns a Domain Class. It alos reverses the PrevBlockHash and merkle tree into human-readable format
     */
    public HeaderReadOnly toBean() {
        HeaderBean result = new HeaderBean((AbstractBlock) null);

        result.setTime(this.creationTimestamp);
        result.setDifficultyTarget(this.difficultyTarget);
        result.setNonce(this.nonce);
        result.setPrevBlockHash(Sha256Hash.wrapReversed(this.prevBlockHash.getHashBytes()));
        result.setVersion(this.version);
        result.setMerkleRoot(Sha256Hash.wrapReversed(this.merkleRoot.getHashBytes()));
        //result.setHash(Sha256Hash.wrapReversed(this.hash.getHashBytes()));

        return result;
    }

    @Override
    public String toString() {
        return "BlockHeaderMsg(hash=" + this.getHash() + ", version=" + this.getVersion() + ", prevBlockHash=" + this.getPrevBlockHash() + ", merkleRoot=" + this.getMerkleRoot() + ", creationTimestamp=" + this.getCreationTimestamp() + ", difficultyTarget=" + this.getDifficultyTarget() + ", nonce=" + this.getNonce() + ", transactionCount=" + this.getTransactionCount() + ")";
    }

    public HashMsg getHash() {
        return hash;
    }

    public long getVersion() {
        return version;
    }

    public HashMsg getPrevBlockHash() {
        return prevBlockHash;
    }

    public HashMsg getMerkleRoot() {
        return merkleRoot;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getDifficultyTarget() {
        return difficultyTarget;
    }

    public long getNonce() {
        return nonce;
    }

    public VarIntMsg getTransactionCount() {
        return transactionCount;
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

        BlockHeaderMsg other = (BlockHeaderMsg) obj;

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
        protected HashMsg hash;
        protected long version;
        protected HashMsg prevBlockHash;
        protected HashMsg merkleRoot;
        protected long creationTimestamp;
        protected long difficultyTarget;
        protected long nonce;
        private VarIntMsg transactionCount;

        public BlockHeaderMsgBuilder hash(HashMsg hash) {
            this.hash = hash;
            return this;
        }

        public BlockHeaderMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public BlockHeaderMsgBuilder prevBlockHash(HashMsg prevBlockHash) {
            this.prevBlockHash = prevBlockHash;
            return this;
        }

        public BlockHeaderMsgBuilder merkleRoot(HashMsg merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public BlockHeaderMsgBuilder creationTimestamp(long creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public BlockHeaderMsgBuilder difficultyTarget(long difficultyTarget) {
            this.difficultyTarget = difficultyTarget;
            return this;
        }

        public BlockHeaderMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public BlockHeaderMsgBuilder transactionCount(long transactionCount) {
            return transactionCount(VarIntMsg.builder().value(transactionCount).build());
        }

        public BlockHeaderMsgBuilder transactionCount(VarIntMsg transactionCount) {
            this.transactionCount = transactionCount;
            return this;
        }

        public BlockHeaderMsg build() {
            return new BlockHeaderMsg(hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce, transactionCount);
        }
    }
}
