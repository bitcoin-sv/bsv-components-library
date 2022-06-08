package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.AbstractBlock;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

import java.io.Serializable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * The Block Header represents the Header of information wihtint a Block.
 * This "Simple" version of the BloclHeaderMSg does NOT include the last field: transactionCount, which includes the
 * number of Txs within the block. That fiel is cinluded in the BlockHeaderMsg class.
 *
 */
public final class BlockHeaderSimpleMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "BlockHeaderSimple";

    public static final int TIMESTAMP_LENGTH = 4;

    public static final int NONCE_LENGTH = 4;

    // IMPORTANT: This field (hash) is NOT SERIALIZED.
    // The hash of the block is NOT part of the BLOCK Message itself: its external to it.
    // In order to calculate a Block Hash we need to serialize the Block first, so instead of doing
    // that avery time we need a Hash, we store the Hash here, at the moment when we deserialize the
    // Block for the first time, so its available for further use.
    protected final Sha256Hash hash;

    protected final long version;
    protected final HashMsg prevBlockHash;
    protected final HashMsg merkleRoot;
    protected final long creationTimestamp;
    protected final long difficultyTarget;
    protected final long nonce;

    public BlockHeaderSimpleMsg(Sha256Hash hash,
                                long version,
                                HashMsg prevBlockHash,
                                HashMsg merkleRoot,
                                long creationTimestamp,
                                long difficultyTarget,
                                long nonce) {
        this.hash = hash;
        this.version = version;
        this.prevBlockHash = prevBlockHash;
        this.merkleRoot = merkleRoot;
        this.creationTimestamp = creationTimestamp;
        this.difficultyTarget = difficultyTarget;
        this.nonce = nonce;
        init();
    }

    public static BlockHeaderSimpleMsgBuilder builder() {
        return new BlockHeaderSimpleMsgBuilder();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
        return 4 + prevBlockHash.getLengthInBytes() + merkleRoot.getLengthInBytes() +
            TIMESTAMP_LENGTH + TIMESTAMP_LENGTH + NONCE_LENGTH;
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
        if (this.hash != null) {
            result.setHash(this.hash);
        }


        return result;
    }

    @Override
    public String toString() {
        return "BlockHeaderMsg(hash=" + this.getHash() + ", version=" + this.getVersion() + ", prevBlockHash=" + this.getPrevBlockHash() + ", merkleRoot=" + this.getMerkleRoot() + ", creationTimestamp=" + this.getCreationTimestamp() + ", difficultyTarget=" + this.getDifficultyTarget() + ", nonce=" + this.getNonce() + ")";
    }

    public Sha256Hash getHash()             { return hash; }
    public long getVersion()                { return version; }
    public HashMsg getPrevBlockHash()       { return prevBlockHash; }
    public HashMsg getMerkleRoot()          { return merkleRoot; }
    public long getCreationTimestamp()      { return creationTimestamp; }
    public long getDifficultyTarget()       { return difficultyTarget; }
    public long getNonce()                  { return nonce; }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BlockHeaderSimpleMsg other = (BlockHeaderSimpleMsg) obj;
        // The "hash" field is calculated, so its not included here:
        return Objects.equal(this.version, other.version)
                && Objects.equal(this.prevBlockHash, other.prevBlockHash)
                && Objects.equal(this.merkleRoot, other.merkleRoot)
                && Objects.equal(this.creationTimestamp, other.creationTimestamp)
                && Objects.equal(this.difficultyTarget, other.difficultyTarget)
                && Objects.equal(this.nonce, other.nonce);
    }

    @Override
    public int hashCode() {
        // The "hash" field is calculated, so its not included here:
        return Objects.hashCode(super.hashCode(), this.version, this.prevBlockHash, this.merkleRoot, this.creationTimestamp, this.difficultyTarget, this.nonce);
    }

    public BlockHeaderSimpleMsgBuilder toBuilder() {
        return new BlockHeaderSimpleMsgBuilder()
                    .hash(this.hash)
                    .version(this.version)
                    .prevBlockHash(this.prevBlockHash)
                    .merkleRoot(this.merkleRoot)
                    .creationTimestamp(this.creationTimestamp)
                    .difficultyTarget(this.difficultyTarget)
                    .nonce(this.nonce);
    }
    /**
     * Builder
     */
    public static class BlockHeaderSimpleMsgBuilder {
        protected Sha256Hash hash;
        protected long version;
        protected HashMsg prevBlockHash;
        protected HashMsg merkleRoot;
        protected long creationTimestamp;
        protected long difficultyTarget;
        protected long nonce;

        public BlockHeaderSimpleMsgBuilder() {}

        public BlockHeaderSimpleMsgBuilder hash(Sha256Hash hash) {
            this.hash = hash;
            return this;
        }

        public BlockHeaderSimpleMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public BlockHeaderSimpleMsgBuilder prevBlockHash(HashMsg prevBlockHash) {
            this.prevBlockHash = prevBlockHash;
            return this;
        }

        public BlockHeaderSimpleMsgBuilder merkleRoot(HashMsg merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public BlockHeaderSimpleMsgBuilder creationTimestamp(long creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public BlockHeaderSimpleMsgBuilder difficultyTarget(long difficultyTarget) {
            this.difficultyTarget = difficultyTarget;
            return this;
        }

        public BlockHeaderSimpleMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public BlockHeaderSimpleMsg build() {
            return new BlockHeaderSimpleMsg(hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce);
        }
    }
}
