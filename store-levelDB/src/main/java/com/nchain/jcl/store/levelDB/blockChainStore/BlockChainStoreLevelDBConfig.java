package com.nchain.jcl.store.levelDB.blockChainStore;


import com.nchain.jcl.store.blockChainStore.validation.BlockChainStoreRuleConfig;
import com.nchain.jcl.store.blockChainStore.validation.rules.BlockChainRule;
import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValueConfig;
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig;
import com.nchain.jcl.tools.config.RuntimeConfig;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.blockchain.pow.factory.RuleCheckerFactory;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockChainStoreLevelDB Imlementation
 */
public class BlockChainStoreLevelDBConfig extends BlockStoreLevelDBConfig implements BlockChainStoreKeyValueConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlockChainStoreLevelDBConfig.class);
    // Default: The Height difference a Fork must have compared tot he main chain to be eligible for prunning
    private static int DEFAULT_FORK_HEIGH_DIFF = 2;
    // Default: The Age of an Orphan Block to be eligible for prunning
    private static Duration DEFAULT_ORPHAN_AGE = Duration.ofMinutes(30);

    private HeaderReadOnly genesisBlock;
    private int         forkPrunningHeightDifference = DEFAULT_FORK_HEIGH_DIFF;
    private boolean     forkPrunningIncludeTxs;
    private Duration    orphanPrunningBlockAge = DEFAULT_ORPHAN_AGE;
    private BlockChainStoreRuleConfig ruleConfig = new BlockChainStoreRuleConfig(Collections.emptyList());

    public BlockChainStoreLevelDBConfig(String id,
                                        Path workingFolder,
                                        RuntimeConfig runtimeConfig,
                                        Integer transactionSize,
                                        String networkId,
                                        HeaderReadOnly genesisBlock,
                                        Integer forkPrunningHeightDifference,
                                        boolean forkPrunningIncludeTxs,
                                        Duration orphanPrunningBlockAge,
                                        BlockChainStoreRuleConfig ruleConfig) {
        super(id, workingFolder, runtimeConfig, transactionSize, networkId);
        this.genesisBlock = genesisBlock;
        if (forkPrunningHeightDifference != null) this.forkPrunningHeightDifference = forkPrunningHeightDifference;
        this.forkPrunningIncludeTxs = forkPrunningIncludeTxs;
        if (orphanPrunningBlockAge != null) this.orphanPrunningBlockAge = orphanPrunningBlockAge;

        if (ruleConfig == null || ruleConfig.getRuleList() == null || ruleConfig.getRuleList().size() == 0){
            log.warn("BlockchainStore has been configured without any block validation rules");
        }

        if (ruleConfig != null) this.ruleConfig = ruleConfig;
    }

    public HeaderReadOnly getGenesisBlock()         { return this.genesisBlock; }
    public int getForkPrunningHeightDifference()    { return this.forkPrunningHeightDifference; }
    public boolean isForkPrunningIncludeTxs()       { return this.forkPrunningIncludeTxs; }
    public Duration getOrphanPrunningBlockAge()     { return this.orphanPrunningBlockAge; }
    public List<BlockChainRule> getBlockChainRules() {return this.ruleConfig.getRuleList(); }

    public static BlockChainStoreLevelDBConfigBuilder chainBuild() {
        return new BlockChainStoreLevelDBConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlockChainStoreLevelDBConfigBuilder {
        private String id;
        private Path workingFolder;
        private RuntimeConfig runtimeConfig;
        private Integer transactionSize;
        private String networkId;
        private HeaderReadOnly genesisBlock;
        private Integer forkPrunningHeightDifference;
        private boolean forkPrunningIncludeTxs;
        private Duration orphanPrunningBlockAge;
        private BlockChainStoreRuleConfig ruleConfig;

        BlockChainStoreLevelDBConfigBuilder() {
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder id(String id) {
            this.id = id;
            return this;
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

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder ruleConfig(BlockChainStoreRuleConfig ruleConfig) {
            this.ruleConfig = ruleConfig;
            return this;
        }

        public BlockChainStoreLevelDBConfig build() {
            return new BlockChainStoreLevelDBConfig(id, workingFolder, runtimeConfig, transactionSize, networkId, genesisBlock, forkPrunningHeightDifference, forkPrunningIncludeTxs, orphanPrunningBlockAge, ruleConfig);
        }
    }
}
