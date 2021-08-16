/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.levelDB.blockStore;


import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockStoreLevelDB Imlementation
 */

public class BlockStoreLevelDBConfig implements BlockStoreKeyValueConfig {

    // Working Folder where the LevelDB files will be stored. Its an inner folder inside the working folder defined
    // by the RuntimeConfiguration
    private static final String LEVELDB_FOLDER = "store";

    // Default DB id:
    private static final String DEFAULT_DB = "level-db";

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

    public BlockStoreLevelDBConfig( String id,
                                    Path workingFolder,
                                    RuntimeConfig runtimeConfig,
                                    Integer transactionBatchSize,
                                    @Nonnull String networkId) {
        this.runtimeConfig = runtimeConfig;
        // The working folder for this BD will be built based on a combination of different parameters:
        // The working folder has priority. If not specified, we use runtime Working folder, with a suffix that might
        // be a default one or the "id" parameter.
        this.workingFolder = (workingFolder != null)
                ? workingFolder
                : (id != null)
                    ? Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), LEVELDB_FOLDER, id)
                    : Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), LEVELDB_FOLDER, DEFAULT_DB);
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
        private String id;
        private Path workingFolder;
        private RuntimeConfig runtimeConfig;
        private Integer transactionBatchSize;
        private @Nonnull String networkId;

        BlockStoreLevelDBConfigBuilder() {
        }

        public BlockStoreLevelDBConfig.BlockStoreLevelDBConfigBuilder id(String id) {
            this.id = id;
            return this;
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
            return new BlockStoreLevelDBConfig(id, workingFolder, runtimeConfig, transactionBatchSize, networkId);
        }
    }
}
