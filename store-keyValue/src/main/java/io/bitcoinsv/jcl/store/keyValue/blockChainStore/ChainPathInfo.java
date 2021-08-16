/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.keyValue.blockChainStore;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-01-19
 */
public final class ChainPathInfo {
    private final int id;
    private final int parent_id;
    private final String blockHash;

    ChainPathInfo(int id, int parent_id, String blockHash) {
        this.id = id;
        this.parent_id = parent_id;
        this.blockHash = blockHash;
    }

    public int getId()              { return this.id; }
    public int getParent_id()       { return this.parent_id; }
    public String getBlockHash()    { return this.blockHash; }

    @Override
    public String toString() {
        return "ChainPathInfo(id=" + this.getId() + ", parent_id=" + this.getParent_id() + ", blockHash=" + this.getBlockHash() + ")";
    }

    public ChainPathInfoBuilder toBuilder() {
        return new ChainPathInfoBuilder().id(this.id).parent_id(this.parent_id).blockHash(this.blockHash);
    }

    public static ChainPathInfoBuilder builder() {
        return new ChainPathInfoBuilder();
    }

    /**
     * Builder
     */
    public static class ChainPathInfoBuilder {
        private int id;
        private int parent_id;
        private String blockHash;

        ChainPathInfoBuilder() {
        }

        public ChainPathInfo.ChainPathInfoBuilder id(int id) {
            this.id = id;
            return this;
        }

        public ChainPathInfo.ChainPathInfoBuilder parent_id(int parent_id) {
            this.parent_id = parent_id;
            return this;
        }

        public ChainPathInfo.ChainPathInfoBuilder blockHash(String blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public ChainPathInfo build() {
            return new ChainPathInfo(id, parent_id, blockHash);
        }
    }
}
