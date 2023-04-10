package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;

import java.io.Serializable;

import static java.util.Optional.ofNullable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * The Block Header represents the Header of information wihtint a Block.
 * It's also used in the HEADERS and GETHEADERS packages.
 */
public final class BlockHeaderMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "BlockHeader";

    private BlockHeaderSimpleMsg blockHeaderSimpleMsg;
    private final VarIntMsg transactionCount;

    public BlockHeaderMsg(Sha256Hash hash, long version, HashMsg prevBlockHash, HashMsg merkleRoot,
                          long creationTimestamp, long difficultyTarget, long nonce, VarIntMsg transactionCount,
                          byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHeaderSimpleMsg = new BlockHeaderSimpleMsg(hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce);
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
        return this.blockHeaderSimpleMsg.calculateLength() + transactionCount.calculateLength();
    }

    @Override
    protected void validateMessage() {
    }

    /**
     * Returns a Domain Class. It also reverses the PrevBlockHash and merkle tree into human-readable format
     */
    public HeaderReadOnly toBean() {
        return blockHeaderSimpleMsg.toBean();
    }

    @Override
    public String toString() {
        return "BlockHeaderMsg(hash=" + this.getHash()
                + ", version=" + blockHeaderSimpleMsg.getVersion()
                + ", prevBlockHash=" + blockHeaderSimpleMsg.getPrevBlockHash()
                + ", merkleRoot=" + blockHeaderSimpleMsg.getMerkleRoot()
                + ", creationTimestamp=" + blockHeaderSimpleMsg.getCreationTimestamp()
                + ", difficultyTarget=" + blockHeaderSimpleMsg.getDifficultyTarget()
                + ", nonce=" + blockHeaderSimpleMsg.getNonce()
                + ", transactionCount=" + transactionCount + ")";
    }

    public Sha256Hash getHash()                         { return blockHeaderSimpleMsg.getHash(); }
    public long getVersion()                            { return blockHeaderSimpleMsg.getVersion();}
    public HashMsg getPrevBlockHash()                   { return blockHeaderSimpleMsg.getPrevBlockHash();}
    public HashMsg getMerkleRoot()                      { return blockHeaderSimpleMsg.getMerkleRoot();}
    public long getCreationTimestamp()                  { return blockHeaderSimpleMsg.getCreationTimestamp();}
    public long getDifficultyTarget()                   { return blockHeaderSimpleMsg.getDifficultyTarget(); }
    public long getNonce()                              { return blockHeaderSimpleMsg.getNonce(); }
    public BlockHeaderSimpleMsg getBlockHeaderSimple()  { return this.blockHeaderSimpleMsg; }
    public VarIntMsg getTransactionCount()              { return transactionCount; }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BlockHeaderMsg other = (BlockHeaderMsg) obj;
        return Objects.equal(this.blockHeaderSimpleMsg, other.blockHeaderSimpleMsg)
                && Objects.equal(this.transactionCount, other.transactionCount);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), this.blockHeaderSimpleMsg, this.transactionCount);
    }

    public BlockHeaderMsgBuilder toBuilder() {
        return new BlockHeaderMsgBuilder(super.extraBytes, super.checksum)
                    .hash(this.blockHeaderSimpleMsg.getHash())
                    .version(this.blockHeaderSimpleMsg.getVersion())
                    .prevBlockHash(this.blockHeaderSimpleMsg.getPrevBlockHash())
                    .merkleRoot(this.blockHeaderSimpleMsg.merkleRoot)
                    .creationTimestamp(this.blockHeaderSimpleMsg.getCreationTimestamp())
                    .difficultyTarget(this.blockHeaderSimpleMsg.getDifficultyTarget())
                    .nonce(this.blockHeaderSimpleMsg.getNonce())
                    .transactionCount(this.transactionCount);
    }
    /**
     * Builder
     */
    public static class BlockHeaderMsgBuilder extends BodyMessageBuilder {
        protected Sha256Hash hash;
        protected long version;
        protected HashMsg prevBlockHash;
        protected HashMsg merkleRoot;
        protected long creationTimestamp;
        protected long difficultyTarget;
        protected long nonce;
        private VarIntMsg transactionCount;

        public BlockHeaderMsgBuilder() {}
        public BlockHeaderMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public BlockHeaderMsgBuilder hash(Sha256Hash hash) {
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
            return new BlockHeaderMsg(hash, version, prevBlockHash, merkleRoot, creationTimestamp, difficultyTarget, nonce, transactionCount, super.extraBytes, super.checksum);
        }
    }
}
