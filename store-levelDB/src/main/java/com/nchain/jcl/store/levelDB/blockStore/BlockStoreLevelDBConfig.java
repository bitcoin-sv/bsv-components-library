package com.nchain.jcl.store.levelDB.blockStore;

import com.nchain.jcl.base.tools.config.RuntimeConfig;
import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockStoreLevelDB Imlementation
 */

@Getter
public class BlockStoreLevelDBConfig {
    // Working Folder: The DB info will be created inside this folder
    private final Path workingFolder;
    // Runtime Config:
    private final RuntimeConfig runtimeConfig;

    @Builder(toBuilder = true)
    public BlockStoreLevelDBConfig(Path workingFolder, RuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.workingFolder = (workingFolder != null)? workingFolder : runtimeConfig.getFileUtils().getRootPath();
    }
}
