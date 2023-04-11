package io.bitcoinsv.bsvcl.common.blobStore;

import java.nio.file.Path;

/**
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 *
 * @author m.fletcher@nchain.com
 * @date 16/03/2022
 */
public class BlockStorePosixConfig{
    private Path workingFolder;
    private int batchSize;

    public BlockStorePosixConfig(Path workingFolder, int batchSize) {
        this.workingFolder = workingFolder;
        this.batchSize = batchSize;
    }

    public Path getWorkingFolder(){
        return workingFolder;
    }

    public int getBatchSize() { return batchSize; }

    public static BlockStorePosixConfigBuilder builder() {
        return new BlockStorePosixConfigBuilder();
    }

    public static class BlockStorePosixConfigBuilder {
        private Path workingFolder;
        private int batchSize;

        public BlockStorePosixConfig.BlockStorePosixConfigBuilder workingFolder(Path workingFolder) {
            this.workingFolder = workingFolder;
            return this;
        }

        public BlockStorePosixConfig.BlockStorePosixConfigBuilder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public BlockStorePosixConfig build() {
            return new BlockStorePosixConfig(workingFolder, batchSize);
        }
    }
}
