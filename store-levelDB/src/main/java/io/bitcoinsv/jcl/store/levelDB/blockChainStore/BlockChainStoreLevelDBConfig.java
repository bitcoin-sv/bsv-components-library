package io.bitcoinsv.jcl.store.levelDB.blockChainStore;


import io.bitcoinsv.jcl.store.blockChainStore.validation.BlockChainStoreRuleConfig;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.BlockChainRule;
import io.bitcoinsv.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValueConfig;
import io.bitcoinsv.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
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

    // This is the Root of all the Block in the DB:
    private HeaderReadOnly genesisBlock;

    // Fork-Prunning Parameters:
    private boolean forkPruningAutomaticEnabled;
    private boolean forkPruningAlertEnabled;
    private Duration forkPruningFrequency;
    private int forkPruningHeightDifference;

    // Orphan-Prunning Frequency:
    private boolean automaticOrphanPruningEnabled;
    private boolean forkPruningIncludeTxs;
    private Duration orphanPruningFrequency;
    private Duration orphanPruningBlockAge;

    private BlockChainStoreRuleConfig ruleConfig = new BlockChainStoreRuleConfig(Collections.emptyList());

    public BlockChainStoreLevelDBConfig(String id,
                                        Path workingFolder,
                                        RuntimeConfig runtimeConfig,
                                        Integer transactionSize,
                                        String networkId,
                                        HeaderReadOnly genesisBlock,

                                        // Fork-Pruning:
                                        boolean forkPruningAutomaticEnabled,
                                        boolean forkPruningAlertEnabled,
                                        Integer forkPruningHeightDifference,
                                        boolean forkPruningIncludeTxs,
                                        Duration forkPruningFrequency,

                                        // Orphan-Pruning:
                                        boolean orphanPruningAutomaticEnabled,
                                        Duration orphanPruningBlockAge,
                                        Duration orphanPruningFrequency,

                                        BlockChainStoreRuleConfig ruleConfig) {

        super(id, workingFolder, runtimeConfig, transactionSize, networkId);
        this.genesisBlock = genesisBlock;

        this.forkPruningAutomaticEnabled = forkPruningAutomaticEnabled;
        this.forkPruningAlertEnabled = forkPruningAlertEnabled;

        this.forkPruningIncludeTxs = forkPruningIncludeTxs;
        this.forkPruningFrequency = forkPruningFrequency;
        this.forkPruningHeightDifference = forkPruningHeightDifference;

        this.automaticOrphanPruningEnabled = orphanPruningAutomaticEnabled;
        this.orphanPruningBlockAge = orphanPruningBlockAge;
        this.orphanPruningFrequency = orphanPruningFrequency;

        if (ruleConfig == null || ruleConfig.getRuleList() == null || ruleConfig.getRuleList().size() == 0){
            log.warn("BlockchainStore has been configured without any block validation rules");
        }

        if (ruleConfig != null) this.ruleConfig = ruleConfig;
    }

    public HeaderReadOnly getGenesisBlock()             { return this.genesisBlock; }

    // Fork-Prunning:
    public boolean isForkPruningAutomaticEnabled()     { return this.forkPruningAutomaticEnabled;}
    public boolean isForkPruningAlertEnabled()         { return this.forkPruningAlertEnabled;}
    public int getForkPruningHeightDifference()        { return this.forkPruningHeightDifference; }
    public boolean isForkPruningIncludeTxs()           { return this.forkPruningIncludeTxs; }
    public Duration getForkPruningFrequency()          { return this.forkPruningFrequency; }

    // Orphan-Prunning:
    public boolean isOrphanPruningAutomaticEnabled()   { return this.automaticOrphanPruningEnabled;}
    public Duration getOrphanPruningBlockAge()         { return this.orphanPruningBlockAge; }
    public Duration getOrphanPruningFrequency()        { return this.orphanPruningFrequency;}

    public List<BlockChainRule> getBlockChainRules()    { return this.ruleConfig.getRuleList(); }

    public static BlockChainStoreLevelDBConfigBuilder chainBuild() {
        return new BlockChainStoreLevelDBConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlockChainStoreLevelDBConfigBuilder {


        // Default: The Height difference a Fork must have compared to the main chain to be eligible for prunning
        private static int DEFAULT_FORK_HEIGH_DIFF = 2;

        // Default: The Age of an Orphan Block to be eligible for prunning
        private static Duration DEFAULT_ORPHAN_AGE = Duration.ofMinutes(30);

        // Automatic prunning configuration:
        public static Duration FORK_PRUNING_FREQUENCY_DEFAULT = Duration.ofMinutes(180);
        public static Duration ORPHAN_PRUNING_FREQUENCY_DEFAULT = Duration.ofMinutes(60);

        private String id;
        private Path workingFolder;
        private RuntimeConfig runtimeConfig;
        private Integer transactionSize;
        private String networkId;
        private HeaderReadOnly genesisBlock;

        // Fork-Prunning Parameters:
        private boolean forkPruningAutomaticEnabled;
        private boolean forkPruningAlertEnabled;
        private Duration forkPruningFrequency = FORK_PRUNING_FREQUENCY_DEFAULT;
        private int forkPruningHeightDifference = DEFAULT_FORK_HEIGH_DIFF;

        // Orphan-Prunning Frequency:
        private boolean orphanPruningAutomaticEnabled;
        private boolean forkPruningIncludeTxs;
        private Duration orphanPruningFrequency = ORPHAN_PRUNING_FREQUENCY_DEFAULT;
        private Duration orphanPruningBlockAge = DEFAULT_ORPHAN_AGE;

        private BlockChainStoreRuleConfig ruleConfig;

        BlockChainStoreLevelDBConfigBuilder() {}

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

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder forkPruningAutomaticEnabled(boolean forkPruningAutomaticEnabled) {
            this.forkPruningAutomaticEnabled = forkPruningAutomaticEnabled;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder forkPruningAlertEnabled(boolean forkPruningAlertEnabled) {
            this.forkPruningAlertEnabled = forkPruningAlertEnabled;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder orphanPruningAutomaticEnabled(boolean orphanPruningAutomaticEnabled) {
            this.orphanPruningAutomaticEnabled = orphanPruningAutomaticEnabled;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder forkPruningHeightDifference(Integer forkPruningHeightDifference) {
            this.forkPruningHeightDifference = forkPruningHeightDifference;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder forkPruningIncludeTxs(boolean forkPruningIncludeTxs) {
            this.forkPruningIncludeTxs = forkPruningIncludeTxs;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder orphanPruningBlockAge(Duration orphanPruningBlockAge) {
            this.orphanPruningBlockAge = orphanPruningBlockAge;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder ruleConfig(BlockChainStoreRuleConfig ruleConfig) {
            this.ruleConfig = ruleConfig;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder forkPruningFrequency(Duration forkPruningFrequency) {
            this.forkPruningFrequency = forkPruningFrequency;
            return this;
        }

        public BlockChainStoreLevelDBConfig.BlockChainStoreLevelDBConfigBuilder orphanPruningFrequency(Duration orphanPruningFrequency) {
            this.orphanPruningFrequency = orphanPruningFrequency;
            return this;
        }

        public BlockChainStoreLevelDBConfig build() {
            return new BlockChainStoreLevelDBConfig(id,
                    workingFolder,
                    runtimeConfig,
                    transactionSize,
                    networkId,
                    genesisBlock,

                    forkPruningAutomaticEnabled,
                    forkPruningAlertEnabled,
                    forkPruningHeightDifference,
                    forkPruningIncludeTxs,
                    forkPruningFrequency,

                    orphanPruningAutomaticEnabled,
                    orphanPruningBlockAge,
                    orphanPruningFrequency,

                    ruleConfig);
        }
    }
}