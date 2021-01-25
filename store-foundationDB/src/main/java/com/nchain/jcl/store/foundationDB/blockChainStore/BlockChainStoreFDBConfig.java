package com.nchain.jcl.store.foundationDB.blockChainStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig;
import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValueConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.nio.file.Path;
import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockChainStoreFoundationDB Imlementation
 */
@Getter
public class BlockChainStoreFDBConfig extends BlockStoreFDBConfig implements BlockChainStoreKeyValueConfig {

    // Default: The Height difference a Fork must have compared tot he main chain to be eligible for prunning
    private static int DEFAULT_FORK_HEIGH_DIFF = 2;
    // Default: The Age of an Orphan Block to be eligible for prunning
    private static Duration DEFAULT_ORPHAN_AGE = Duration.ofMinutes(30);

    private BlockHeader genesisBlock;
    private int         forkPrunningHeightDifference = DEFAULT_FORK_HEIGH_DIFF;
    private boolean     forkPrunningIncludeTxs;
    private Duration    orphanPrunningBlockAge = DEFAULT_ORPHAN_AGE;

    @Builder(builderMethodName = "chainBuild")
    public BlockChainStoreFDBConfig(RuntimeConfig runtimeConfig,
                                    String clusterFile,
                                    Integer apiVersion,
                                    Integer transactionSize,
                                    String networkId,
                                    BlockHeader genesisBlock,
                                    Integer forkPrunningHeightDifference,
                                    boolean forkPrunningIncludeTxs,
                                    Duration orphanPrunningBlockAge) {
        super(runtimeConfig, clusterFile, apiVersion, networkId, transactionSize);
        this.genesisBlock = genesisBlock;
        if (forkPrunningHeightDifference != null) this.forkPrunningHeightDifference = forkPrunningHeightDifference;
        this.forkPrunningIncludeTxs = forkPrunningIncludeTxs;
        if (orphanPrunningBlockAge != null) this.orphanPrunningBlockAge = orphanPrunningBlockAge;
    }
}
