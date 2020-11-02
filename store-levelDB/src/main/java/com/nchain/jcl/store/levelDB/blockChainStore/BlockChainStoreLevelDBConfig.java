package com.nchain.jcl.store.levelDB.blockChainStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig;
import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockChainStoreLevelDB Imlementation
 */

@Getter
public class BlockChainStoreLevelDBConfig extends BlockStoreLevelDBConfig {
    private final BlockHeader genesisBlock;

    @Builder(builderMethodName = "chainBuild")
    public BlockChainStoreLevelDBConfig(Path workingFolder,
                                        RuntimeConfig runtimeConfig,
                                        BlockHeader genesisBlock) {
        super(workingFolder, runtimeConfig);
        this.genesisBlock = genesisBlock;
    }
}
