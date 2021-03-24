package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;
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
public abstract class BlockHeaderMsg extends Message {

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
    protected final VarIntMsg transactionCount;

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

    @Override
    protected long calculateLength() {
        return 4 + prevBlockHash.getLengthInBytes() + merkleRoot.getLengthInBytes() +
            TIMESTAMP_LENGTH + TIMESTAMP_LENGTH + NONCE_LENGTH + transactionCount.getLengthInBytes();
    }

    @Override
    protected void validateMessage() {
    }

    /**
     * Returns a Domain Class
     */
    public HeaderReadOnly toBean() {
        HeaderBean result = new HeaderBean((AbstractBlock) null);
        result.setTime(this.creationTimestamp);
        result.setDifficultyTarget(this.difficultyTarget);
        result.setNonce(this.nonce);
        result.setPrevBlockHash(Sha256Hash.wrap(this.prevBlockHash.getHashBytes()));
        result.setVersion(this.version);
        result.setMerkleRoot(Sha256Hash.wrap(this.merkleRoot.getHashBytes()));
        result.setHash(Sha256Hash.wrap(this.hash.getHashBytes()));
        return result;
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

    public HashMsg getHash() {
        return this.hash;
    }

    public long getVersion() {
        return this.version;
    }

    public HashMsg getPrevBlockHash() {
        return this.prevBlockHash;
    }

    public HashMsg getMerkleRoot() {
        return this.merkleRoot;
    }

    public long getCreationTimestamp() {
        return this.creationTimestamp;
    }

    public long getDifficultyTarget() {
        return this.difficultyTarget;
    }

    public long getNonce() {
        return this.nonce;
    }

    public VarIntMsg getTransactionCount() {
        return this.transactionCount;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce, transactionCount);
    }
}
