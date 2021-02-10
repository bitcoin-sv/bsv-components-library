package com.nchain.jcl.store.levelDB.blockChainStore;


import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValueConfig;
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig;
import com.nchain.jcl.tools.config.RuntimeConfig;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;

import java.nio.file.Path;
import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockChainStoreLevelDB Imlementation
 */
public class BlockChainStoreLevelDBConfig extends BlockStoreLevelDBConfig implements BlockChainStoreKeyValueConfig {

    // Default: The Height difference a Fork must have compared tot he main chain to be eligible for prunning
    private static int DEFAULT_FORK_HEIGH_DIFF = 2;
    // Default: The Age of an Orphan Block to be eligible for prunning
    private static Duration DEFAULT_ORPHAN_AGE = Duration.ofMinutes(30);

    private HeaderReadOnly genesisBlock;
    private int         forkPrunningHeightDifference = DEFAULT_FORK_HEIGH_DIFF;
    private boolean     forkPrunningIncludeTxs;
    private Duration    orphanPrunningBlockAge = DEFAULT_ORPHAN_AGE;

    public BlockChainStoreLevelDBConfig(Path workingFolder,
                                        RuntimeConfig runtimeConfig,
                                        Integer transactionSize,
                                        String networkId,
                                        HeaderReadOnly genesisBlock,
                                        Integer forkPrunningHeightDifference,
                                        boolean forkPrunningIncludeTxs,
                                        Duration orphanPrunningBlockAge) {
        super(workingFolder, runtimeConfig, transactionSize, networkId);
        this.genesisBlock = genesisBlock;
        if (forkPrunningHeightDifference != null) this.forkPrunningHeightDifference = forkPrunningHeightDifference;
        this.forkPrunningIncludeTxs = forkPrunningIncludeTxs;
        if (orphanPrunningBlockAge != null) this.orphanPrunningBlockAge = orphanPrunningBlockAge;
    }

    public HeaderReadOnly getGenesisBlock()         { return this.genesisBlock; }
    public int getForkPrunningHeightDifference()    { return this.forkPrunningHeightDifference; }
    public boolean isForkPrunningIncludeTxs()       { return this.forkPrunningIncludeTxs; }
    public Duration getOrphanPrunningBlockAge()     { return this.orphanPrunningBlockAge; }

    public static BlockChainStoreLevelDBConfigBuilder chainBuild() {
        return new BlockChainStoreLevelDBConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlockChainStoreLevelDBConfigBuilder {
        private Path workingFolder;
        private RuntimeConfig runtimeConfig;
        private Integer transactionSize;
        private String networkId;
        private HeaderReadOnly genesisBlock;
        private Integer forkPrunningHeightDifference;
        private boolean forkPrunningIncludeTxs;
        private Duration orphanPrunningBlockAge;

        BlockChainStoreLevelDBConfigBuilder() {
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder workingFolder(Path workingFolder) {
            this.workingFolder = workingFolder;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder runtimeConfig(RuntimeConfig runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder transactionSize(Integer transactionSize) {
            this.transactionSize = transactionSize;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder genesisBlock(HeaderReadOnly genesisBlock) {
            this.genesisBlock = genesisBlock;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder forkPrunningHeightDifference(Integer forkPrunningHeightDifference) {
            this.forkPrunningHeightDifference = forkPrunningHeightDifference;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder forkPrunningIncludeTxs(boolean forkPrunningIncludeTxs) {
            this.forkPrunningIncludeTxs = forkPrunningIncludeTxs;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder orphanPrunningBlockAge(Duration orphanPrunningBlockAge) {
            this.orphanPrunningBlockAge = orphanPrunningBlockAge;
            return this;
        }

        public BlockChainStoreLevelDBConfig build() {
            return new BlockChainStoreLevelDBConfig(workingFolder, runtimeConfig, transactionSize, networkId, genesisBlock, forkPrunningHeightDifference, forkPrunningIncludeTxs, orphanPrunningBlockAge);
        }
    }
}
