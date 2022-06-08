package io.bitcoinsv.jcl.store.keyValue.blockChainStore;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores info about the Chain, relative to the position of one specific Block.
 * This class is an alternative version of the {@link io.bitcoinj.bitcoin.api.extended.ChainInfo} class,
 * storing the same basic info, but this class does not store the whole BlockHeader, instead it only stored the Hash.
 * This class is used to store the relative Chain info for each Block. Sicne the Blocks themselves are
 * already being stored in other keys, we only need the Block Hash here.
 *
 * If a Block is "Connected" to a Chain, then there is an entry ("b_chain:[blockHash]")for that block, storing this
 * instane as the value (for there are at least 2 entries for that block, the "regular" one taht stores the block
 * itself, and this one).
 *
 * NOTE: A block is "connected" to a Chain if there is an entry for that Block containing this info.
 */
public final class BlockChainInfo implements Serializable {
    private final String blockHash;
    private final BigInteger chainWork;
    private final int height;
    private final long totalChainSize;
    private final int chainPathId;

    BlockChainInfo(String blockHash, BigInteger chainWork, int height, long totalChainSize, int chainPathId) {
        this.blockHash = blockHash;
        this.chainWork = chainWork;
        this.height = height;
        this.totalChainSize = totalChainSize;
        this.chainPathId = chainPathId;
    }

    public String getBlockHash()        { return this.blockHash; }
    public BigInteger getChainWork()    { return this.chainWork; }
    public int getHeight()              { return this.height; }
    public long getTotalChainSize()     { return this.totalChainSize; }
    public int getChainPathId()         { return this.chainPathId; }

    @Override
    public String toString() {
        return "BlockChainInfo(blockHash=" + this.getBlockHash() + ", chainWork=" + this.getChainWork() + ", height=" + this.getHeight() + ", totalChainSize=" + this.getTotalChainSize() + ", chainPathId=" + this.getChainPathId() + ")";
    }

    public BlockChainInfoBuilder toBuilder() {
        return new BlockChainInfoBuilder().blockHash(this.blockHash).chainWork(this.chainWork).height(this.height).totalChainSize(this.totalChainSize).chainPathId(this.chainPathId);
    }

    public static BlockChainInfoBuilder builder() {
        return new BlockChainInfoBuilder();
    }

    /**
     * Builder
     */
    public static class BlockChainInfoBuilder {
        private String blockHash;
        private BigInteger chainWork;
        private int height;
        private long totalChainSize;
        private int chainPathId;

        BlockChainInfoBuilder() {
        }

        public BlockChainInfo.BlockChainInfoBuilder blockHash(String blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public BlockChainInfo.BlockChainInfoBuilder chainWork(BigInteger chainWork) {
            this.chainWork = chainWork;
            return this;
        }

        public BlockChainInfo.BlockChainInfoBuilder height(int height) {
            this.height = height;
            return this;
        }

        public BlockChainInfo.BlockChainInfoBuilder totalChainSize(long totalChainSize) {
            this.totalChainSize = totalChainSize;
            return this;
        }

        public BlockChainInfo.BlockChainInfoBuilder chainPathId(int chainPathId) {
            this.chainPathId = chainPathId;
            return this;
        }

        public BlockChainInfo build() {
            return new BlockChainInfo(blockHash, chainWork, height, totalChainSize, chainPathId);
        }

    }
}
