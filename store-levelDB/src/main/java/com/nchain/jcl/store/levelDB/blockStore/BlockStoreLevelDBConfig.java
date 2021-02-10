package com.nchain.jcl.store.levelDB.blockStore;


import com.nchain.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;
import com.nchain.jcl.tools.config.RuntimeConfig;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockStoreLevelDB Imlementation
 */

public class BlockStoreLevelDBConfig implements BlockStoreKeyValueConfig {

    // Transaction BATCH Default Size
    private static final int TRANSACTION_BATCH_SIZE = 5000;

    /** Maximun number of Items that can be processed in a single DB Transaction */
    private final int transactionBatchSize;

    /** Working Folder: The DB info will be created inside this folder */
    private final Path workingFolder;

    /** Runtime Config */
    private final RuntimeConfig runtimeConfig;

    /**
     * The network Id to use as a Base Directory. We use a String here to keep dependencies simple,
     * but in real scenarios this value will be obtained from a ProtocolConfiguration form the JCL-Net
     * module
     */
    private String networkId;

    public BlockStoreLevelDBConfig(Path workingFolder,
                                   RuntimeConfig runtimeConfig,
                                   Integer transactionBatchSize,
                                   @Nonnull String networkId) {
        this.runtimeConfig = runtimeConfig;
        this.workingFolder = (workingFolder != null)? workingFolder : runtimeConfig.getFileUtils().getRootPath();
        this.transactionBatchSize = (transactionBatchSize != null) ? transactionBatchSize : TRANSACTION_BATCH_SIZE;
        this.networkId = networkId;
    }

    public int getTransactionBatchSize()    { return this.transactionBatchSize; }
    public Path getWorkingFolder()          { return this.workingFolder; }
    public RuntimeConfig getRuntimeConfig() { return this.runtimeConfig; }
    public String getNetworkId()            { return this.networkId; }

    public static BlockStoreLevelDBConfigBuilder builder() {
        return new BlockStoreLevelDBConfigBuilder();
    }

    public BlockStoreLevelDBConfigBuilder toBuilder() {
        return new BlockStoreLevelDBConfigBuilder().workingFolder(this.workingFolder).runtimeConfig(this.runtimeConfig).transactionBatchSize(this.transactionBatchSize).networkId(this.networkId);
    }

    /**
     * Builder
     */
    public static class BlockStoreLevelDBConfigBuilder {
        private Path workingFolder;
        private RuntimeConfig runtimeConfig;
        private Integer transactionBatchSize;
        private @Nonnull String networkId;

        BlockStoreLevelDBConfigBuilder() {
        }

        public BlockStoreLevelDBConfig.BlockStoreLevelDBConfigBuilder workingFolder(Path workingFolder) {
            this.workingFolder = workingFolder;
            return this;
        }

        public BlockStoreLevelDBConfig.BlockStoreLevelDBConfigBuilder runtimeConfig(RuntimeConfig runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
            return this;
        }

        public BlockStoreLevelDBConfig.BlockStoreLevelDBConfigBuilder transactionBatchSize(Integer transactionBatchSize) {
            this.transactionBatchSize = transactionBatchSize;
            return this;
        }

        public BlockStoreLevelDBConfig.BlockStoreLevelDBConfigBuilder networkId(@Nonnull String networkId) {
            this.networkId = networkId;
            return this;
        }

        public BlockStoreLevelDBConfig build() {
            return new BlockStoreLevelDBConfig(workingFolder, runtimeConfig, transactionBatchSize, networkId);
        }
    }
}
