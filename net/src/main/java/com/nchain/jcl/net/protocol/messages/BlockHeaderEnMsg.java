package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Enriched block headers is embedded in headersen message. This message returns same data as BlockHeader message with
 * the addition of fields for actual number of transactions that are included in the block and proof of inclusion
 * for coinbase transaction along with the whole coinbase transaction.
 */
public final class BlockHeaderEnMsg extends Message implements Serializable {
    public static final String MESSAGE_TYPE = "blockHeaderEn";
    public static final int TIMESTAMP_LENGTH = 4;
    public static final int NBITS_LENGTH = 4;
    public static final int NONCE_LENGTH = 4;
    public static final int NO_MORE_HEAD_LENGTH = 1;
    public static final int HAS_COINBASEDATA_LENGTH = 1;
    public static final int TX_CNT = 8;

    private final long version;
    private final HashMsg prevBlockHash;
    private final HashMsg merkleRoot;
    private final long creationTimestamp;
    private final long nBits;
    private final long nonce;
    private final long transactionCount;
    private final boolean noMoreHeaders;
    private final boolean hasCoinbaseData;
    private final List<HashMsg> coinbaseMerkleProof;
    private final VarStrMsg coinbase;

    //coinbaseTX is not part of the BlockHeaderEnrichedMsg itself
    private final TxMsg coinbaseTX;

    // IMPORTANT: This field (hash) is NOT SERIALIZED.
    // The hash of the block is NOT part of the BLOCK Message itself: its external to it.
    // In order to calculate a Block Hash we need to serialize the Block first, so instead of doing
    // that avery time we need a Hash, we store the Hash here, at the moment when we deserialize the
    // Block for the first time, so its available for further use.
    private final HashMsg hash;

    public BlockHeaderEnMsg(HashMsg hash, long version, HashMsg prevBlockHash, HashMsg merkleRoot, long creationTimestamp,
                            long nBits, long nonce, long transactionCount, boolean noMoreHeaders,
                            boolean hasCoinbaseData, List<HashMsg> coinbaseMerkleProof, VarStrMsg coinbase, TxMsg coinbaseTX) {
        this.hash = hash;
        this.version = version;
        this.prevBlockHash = prevBlockHash;
        this.merkleRoot = merkleRoot;
        this.creationTimestamp = creationTimestamp;
        this.nBits = nBits;
        this.nonce = nonce;
        this.transactionCount = transactionCount;
        this.noMoreHeaders = noMoreHeaders;
        this.hasCoinbaseData = hasCoinbaseData;
        this.coinbaseMerkleProof = coinbaseMerkleProof.stream().collect(Collectors.toUnmodifiableList());
        this.coinbase = coinbase;
        this.coinbaseTX = coinbaseTX;
        init();
    }

    @Override
    protected long calculateLength() {
            long length = 4 + prevBlockHash.getLengthInBytes() + merkleRoot.getLengthInBytes()
                    + TIMESTAMP_LENGTH + NBITS_LENGTH + NONCE_LENGTH + TX_CNT
                    + NO_MORE_HEAD_LENGTH + HAS_COINBASEDATA_LENGTH;

            if(hasCoinbaseData) {
                int size = coinbaseMerkleProof.size();
                length = length + size *  HashMsg.HASH_LENGTH;

                //for unit test purpose
                if(coinbase == null) {
                    length += coinbaseTX != null ? coinbaseTX.getLengthInBytes():0;
                } else {
                    length += coinbase.getLengthInBytes();
                }
             }
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()                  { return MESSAGE_TYPE; }
    public long getVersion()                        { return this.version; }
    public HashMsg getPrevBlockHash()               { return this.prevBlockHash; }
    public HashMsg getMerkleRoot()                  { return this.merkleRoot; }
    public long getCreationTimestamp()              { return this.creationTimestamp; }
    public long getNBits()                          { return this.nBits; }
    public long getNonce()                          { return this.nonce; }
    public long getTransactionCount()               { return this.transactionCount; }
    public boolean isNoMoreHeaders()                { return this.noMoreHeaders; }
    public boolean isHasCoinbaseData()              { return this.hasCoinbaseData; }
    public List<HashMsg> getCoinbaseMerkleProof()   { return this.coinbaseMerkleProof; }
    public VarStrMsg getCoinbase()                  { return this.coinbase; }
    public TxMsg getCoinbaseTX()                    { return this.coinbaseTX; }
    public HashMsg getHash()                        { return this.hash; }

    public String toString() {
        return "BlockHeaderEnMsg(version=" + this.getVersion() + ", prevBlockHash=" + this.getPrevBlockHash() + ", merkleRoot=" + this.getMerkleRoot() + ", creationTimestamp=" + this.getCreationTimestamp() + ", nBits=" + this.getNBits() + ", nonce=" + this.getNonce() + ", transactionCount=" + this.getTransactionCount() + ", noMoreHeaders=" + this.isNoMoreHeaders() + ", hasCoinbaseData=" + this.isHasCoinbaseData() + ", coinbaseMerkleProof=" + this.getCoinbaseMerkleProof() + ", coinbase=" + this.getCoinbase() + ", coinbaseTX=" + this.getCoinbaseTX() + ", hash=" + this.getHash() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version, prevBlockHash, merkleRoot, creationTimestamp, nBits, nonce, transactionCount, noMoreHeaders, hasCoinbaseData, coinbaseMerkleProof, coinbase, coinbaseTX, hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        BlockHeaderEnMsg other = (BlockHeaderEnMsg) obj;
        return Objects.equal(this.version, other.version)
                && Objects.equal(this.prevBlockHash, other.prevBlockHash)
                && Objects.equal(this.merkleRoot, other.merkleRoot)
                && Objects.equal(this.creationTimestamp, other.creationTimestamp)
                && Objects.equal(this.nBits, other.nBits)
                && Objects.equal(this.nonce, other.nonce)
                && Objects.equal(this.transactionCount, other.transactionCount)
                && Objects.equal(this.noMoreHeaders, other.noMoreHeaders)
                && Objects.equal(this.hasCoinbaseData, other.hasCoinbaseData)
                && Objects.equal(this.coinbaseMerkleProof, other.coinbaseMerkleProof)
                && Objects.equal(this.coinbase, other.coinbase)
                && Objects.equal(this.coinbaseTX, other.coinbaseTX)
                && Objects.equal(this.hash, other.hash);
    }

    public static BlockHeaderEnMsgBuilder builder() {
        return new BlockHeaderEnMsgBuilder();
    }

    /**
     * Builder
     */
    public static class BlockHeaderEnMsgBuilder {
        private HashMsg hash;
        private long version;
        private HashMsg prevBlockHash;
        private HashMsg merkleRoot;
        private long creationTimestamp;
        private long nBits;
        private long nonce;
        private long transactionCount;
        private boolean noMoreHeaders;
        private boolean hasCoinbaseData;
        private List<HashMsg> coinbaseMerkleProof;
        private VarStrMsg coinbase;
        private TxMsg coinbaseTX;

        BlockHeaderEnMsgBuilder() {}

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder hash(HashMsg hash) {
            this.hash = hash;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder prevBlockHash(HashMsg prevBlockHash) {
            this.prevBlockHash = prevBlockHash;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder merkleRoot(HashMsg merkleRoot) {
            this.merkleRoot = merkleRoot;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder creationTimestamp(long creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder nBits(long nBits) {
            this.nBits = nBits;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder transactionCount(long transactionCount) {
            this.transactionCount = transactionCount;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder noMoreHeaders(boolean noMoreHeaders) {
            this.noMoreHeaders = noMoreHeaders;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder hasCoinbaseData(boolean hasCoinbaseData) {
            this.hasCoinbaseData = hasCoinbaseData;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder coinbaseMerkleProof(List<HashMsg> coinbaseMerkleProof) {
            this.coinbaseMerkleProof = coinbaseMerkleProof;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder coinbase(VarStrMsg coinbase) {
            this.coinbase = coinbase;
            return this;
        }

        public BlockHeaderEnMsg.BlockHeaderEnMsgBuilder coinbaseTX(TxMsg coinbaseTX) {
            this.coinbaseTX = coinbaseTX;
            return this;
        }

        public BlockHeaderEnMsg build() {
            return new BlockHeaderEnMsg(hash, version, prevBlockHash, merkleRoot, creationTimestamp, nBits, nonce, transactionCount, noMoreHeaders, hasCoinbaseData, coinbaseMerkleProof, coinbase, coinbaseTX);
        }
    }
}
