package com.nchain.jcl.store.foundationDB.blockChainStore;


import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig;
import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValueConfig;
import com.nchain.jcl.tools.config.RuntimeConfig;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockChainStoreFoundationDB Imlementation
 */
public class BlockChainStoreFDBConfig extends BlockStoreFDBConfig implements BlockChainStoreKeyValueConfig {

    // Default: The Height difference a Fork must have compared tot he main chain to be eligible for prunning
    private static int DEFAULT_FORK_HEIGH_DIFF = 2;
    // Default: The Age of an Orphan Block to be eligible for prunning
    private static Duration DEFAULT_ORPHAN_AGE = Duration.ofMinutes(30);

    private HeaderReadOnly genesisBlock;
    private int         forkPrunningHeightDifference = DEFAULT_FORK_HEIGH_DIFF;
    private boolean     forkPrunningIncludeTxs;
    private Duration    orphanPrunningBlockAge = DEFAULT_ORPHAN_AGE;

    public BlockChainStoreFDBConfig(RuntimeConfig runtimeConfig,
                                    String clusterFile,
                                    Integer apiVersion,
                                    Integer transactionSize,
                                    String networkId,
                                    HeaderReadOnly genesisBlock,
                                    Integer forkPrunningHeightDifference,
                                    boolean forkPrunningIncludeTxs,
                                    Duration orphanPrunningBlockAge) {
        super(runtimeConfig, clusterFile, apiVersion, networkId, transactionSize);
        this.genesisBlock = genesisBlock;
        if (forkPrunningHeightDifference != null) this.forkPrunningHeightDifference = forkPrunningHeightDifference;
        this.forkPrunningIncludeTxs = forkPrunningIncludeTxs;
        if (orphanPrunningBlockAge != null) this.orphanPrunningBlockAge = orphanPrunningBlockAge;
    }

    public HeaderReadOnly getGenesisBlock()         { return this.genesisBlock; }
    public int getForkPrunningHeightDifference()    { return this.forkPrunningHeightDifference; }
    public boolean isForkPrunningIncludeTxs()       { return this.forkPrunningIncludeTxs; }
    public Duration getOrphanPrunningBlockAge()     { return this.orphanPrunningBlockAge; }

    public static BlockChainStoreFDBConfigBuilder chainBuild() {
        return new BlockChainStoreFDBConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlockChainStoreFDBConfigBuilder {
        private RuntimeConfig runtimeConfig;
        private String clusterFile;
        private Integer apiVersion;
        private Integer transactionSize;
        private String networkId;
        private HeaderReadOnly genesisBlock;
        private Integer forkPrunningHeightDifference;
        private boolean forkPrunningIncludeTxs;
        private Duration orphanPrunningBlockAge;

        BlockChainStoreFDBConfigBuilder() {
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder runtimeConfig(RuntimeConfig runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder clusterFile(String clusterFile) {
            this.clusterFile = clusterFile;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder apiVersion(Integer apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder transactionSize(Integer transactionSize) {
            this.transactionSize = transactionSize;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder genesisBlock(HeaderReadOnly genesisBlock) {
            this.genesisBlock = genesisBlock;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder forkPrunningHeightDifference(Integer forkPrunningHeightDifference) {
            this.forkPrunningHeightDifference = forkPrunningHeightDifference;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder forkPrunningIncludeTxs(boolean forkPrunningIncludeTxs) {
            this.forkPrunningIncludeTxs = forkPrunningIncludeTxs;
            return this;
        }

        public BlockChainStoreFDBConfig.BlockChainStoreFDBConfigBuilder orphanPrunningBlockAge(Duration orphanPrunningBlockAge) {
            this.orphanPrunningBlockAge = orphanPrunningBlockAge;
            return this;
        }

        public BlockChainStoreFDBConfig build() {
            return new BlockChainStoreFDBConfig(runtimeConfig, clusterFile, apiVersion, transactionSize, networkId, genesisBlock, forkPrunningHeightDifference, forkPrunningIncludeTxs, orphanPrunningBlockAge);
        }

    }
}
