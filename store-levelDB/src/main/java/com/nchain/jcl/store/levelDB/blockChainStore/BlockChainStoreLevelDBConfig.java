package com.nchain.jcl.store.levelDB.blockChainStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValueConfig;
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig;
import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockChainStoreLevelDB Imlementation
 */
@Getter
public class BlockChainStoreLevelDBConfig extends BlockStoreLevelDBConfig implements BlockChainStoreKeyValueConfig {

    // Default: The Height difference a Fork must have compared tot he main chain to be eligible for prunning
    private static int DEFAULT_FORK_HEIGH_DIFF = 2;
    // Default: The Age of an Orphan Block to be eligible for prunning
    private static Duration DEFAULT_ORPHAN_AGE = Duration.ofMinutes(30);

    private BlockHeader genesisBlock;
    private int         forkPrunningHeightDifference = DEFAULT_FORK_HEIGH_DIFF;
    private boolean     forkPrunningIncludeTxs;
    private Duration    orphanPrunningBlockAge = DEFAULT_ORPHAN_AGE;

    @Builder(builderMethodName = "chainBuild")
    public BlockChainStoreLevelDBConfig(Path workingFolder,
                                        RuntimeConfig runtimeConfig,
                                        Integer transactionSize,
                                        String networkId,
                                        BlockHeader genesisBlock,
                                        Integer forkPrunningHeightDifference,
                                        boolean forkPrunningIncludeTxs,
                                        Duration orphanPrunningBlockAge) {
        super(workingFolder, runtimeConfig, transactionSize, networkId);
        this.genesisBlock = genesisBlock;
        if (forkPrunningHeightDifference != null) this.forkPrunningHeightDifference = forkPrunningHeightDifference;
        this.forkPrunningIncludeTxs = forkPrunningIncludeTxs;
        if (orphanPrunningBlockAge != null) this.orphanPrunningBlockAge = orphanPrunningBlockAge;
    }
}
