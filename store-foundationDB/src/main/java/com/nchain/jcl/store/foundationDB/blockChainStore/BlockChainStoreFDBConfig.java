package com.nchain.jcl.store.foundationDB.blockChainStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig;
import com.nchain.jcl.store.keyValue.blockChainStore.BlockChainStoreKeyValueConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockChainStoreFoundationDB Imlementation
 */
@Getter
public class BlockChainStoreFDBConfig extends BlockStoreFDBConfig implements BlockChainStoreKeyValueConfig {
    private BlockHeader genesisBlock;

    @Builder(builderMethodName = "chainBuilder")
    public BlockChainStoreFDBConfig(RuntimeConfig runtimeConfig,
                                    String clusterFile,
                                    Integer apiVersion,
                                    @NonNull String networkId,
                                    Integer transactionBatchSize,
                                    BlockHeader genesisBlock) {
        super(runtimeConfig, clusterFile, apiVersion, networkId, transactionBatchSize);
        this.genesisBlock = genesisBlock;
    }
}
