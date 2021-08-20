/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore;


import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the current State of the BlockChain Store at a point in time
 */
public final class BlockChainStoreState {
    private final List<ChainInfo> tipsChains;
    private final long numBlocks;
    private final long numTxs;

    BlockChainStoreState(List<ChainInfo> tipsChains, long numBlocks, long numTxs) {
        this.tipsChains = tipsChains;
        this.numBlocks = numBlocks;
        this.numTxs = numTxs;
    }

    public List<ChainInfo> getTipsChains()  { return this.tipsChains; }
    public long getNumBlocks()              { return this.numBlocks; }
    public long getNumTxs()                 { return this.numTxs; }

    @Override
    public String toString() {
        return "BlockChainStoreState(tipsChains=" + this.getTipsChains() + ", numBlocks=" + this.getNumBlocks() + ", numTxs=" + this.getNumTxs() + ")";
    }

    public static BlockChainStoreStateBuilder builder() {
        return new BlockChainStoreStateBuilder();
    }

    /**
     * Builder
     */
    public static class BlockChainStoreStateBuilder {
        private List<ChainInfo> tipsChains;
        private long numBlocks;
        private long numTxs;

        BlockChainStoreStateBuilder() {
        }

        public BlockChainStoreState.BlockChainStoreStateBuilder tipsChains(List<ChainInfo> tipsChains) {
            this.tipsChains = tipsChains;
            return this;
        }

        public BlockChainStoreState.BlockChainStoreStateBuilder numBlocks(long numBlocks) {
            this.numBlocks = numBlocks;
            return this;
        }

        public BlockChainStoreState.BlockChainStoreStateBuilder numTxs(long numTxs) {
            this.numTxs = numTxs;
            return this;
        }

        public BlockChainStoreState build() {
            return new BlockChainStoreState(tipsChains, numBlocks, numTxs);
        }
    }
}
