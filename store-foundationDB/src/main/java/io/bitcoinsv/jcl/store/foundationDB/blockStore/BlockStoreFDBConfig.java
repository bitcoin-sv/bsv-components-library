/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.foundationDB.blockStore;

import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault;

import javax.annotation.Nonnull;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the FoundationDB Implementation of the BlockStore interface
 */
public class BlockStoreFDBConfig implements BlockStoreKeyValueConfig {

    /**
     * Number of Items to process on each Transaction. An "item" might be a Block, a Tx, etc.
     * FoundationDB has a limitation on the number of Bytes affected within a Transaction and also on the time it takes
     * for each Transaction tom complete, that means that when running operations on a list of items (Saving Blocks or
     * Tx, removing, etc), we need to make sure that the number of items is not too big. So we use these property to
     * break down the list into smaller sublist, and on Tx is created for each sublist, to handle the items in that
     * sublist.
     *
     * This technique only takes into consideration the number of Items, not their size, so its not very efficient. A
     * more accurate implementation will take into consideration the SIZE of each ITem and will break down the list
     * depending on those sizes. That is doable when insert elements (since the items themselves are in the list so we
     * can inspect them and check out their size), but its more problematic when removing. So this technique is a
     * middle-ground solution.
     */
    public static final int TRANSACTION_BATCH_SIZE = 5000;

    /** Java API Version. This might change if the maven dependency is updated, so be careful */
    private static final int API_VERSION = 510;

    /** Runtime Config */
    private final RuntimeConfig runtimeConfig;

    /** FoundationDb cluster file. If not specified, the default location is used */
    private String clusterFile;

    /** JAva Api version to use */
    private int apiVersion;

    /**
     * The network Id to use as a Base Directory. We use a String here to keep dependencies simple,
     * but in real scenarios this value will be obtained from a ProtocolConfiguration form the JCL-Net
     * module
     */
    private String networkId;

    /**
     * Maximun Number of Items to process in a Transaction
     */
    private int transactionBatchSize;

    public BlockStoreFDBConfig(RuntimeConfig runtimeConfig,
                               String clusterFile,
                               Integer apiVersion,
                               @Nonnull String networkId,
                               Integer transactionBatchSize) {
        this.runtimeConfig = (runtimeConfig != null) ? runtimeConfig: new RuntimeConfigDefault();
        this.clusterFile = clusterFile;
        this.apiVersion = (apiVersion != null) ? apiVersion : API_VERSION;
        this.networkId = networkId;
        this.transactionBatchSize = (transactionBatchSize != null) ? transactionBatchSize : TRANSACTION_BATCH_SIZE;
    }

    public RuntimeConfig getRuntimeConfig() { return this.runtimeConfig; }
    public String getClusterFile()          { return this.clusterFile; }
    public int getApiVersion()              { return this.apiVersion; }
    public String getNetworkId()            { return this.networkId; }
    public int getTransactionBatchSize()    { return this.transactionBatchSize; }

    public static BlockStoreFDBConfigBuilder builder() {
        return new BlockStoreFDBConfigBuilder();
    }

    /**
     * Builder
     */
    public static class BlockStoreFDBConfigBuilder {
        private RuntimeConfig runtimeConfig;
        private String clusterFile;
        private Integer apiVersion;
        private @Nonnull String networkId;
        private Integer transactionBatchSize;

        BlockStoreFDBConfigBuilder() {
        }

        public BlockStoreFDBConfig.BlockStoreFDBConfigBuilder runtimeConfig(RuntimeConfig runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
            return this;
        }

        public BlockStoreFDBConfig.BlockStoreFDBConfigBuilder clusterFile(String clusterFile) {
            this.clusterFile = clusterFile;
            return this;
        }

        public BlockStoreFDBConfig.BlockStoreFDBConfigBuilder apiVersion(Integer apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public BlockStoreFDBConfig.BlockStoreFDBConfigBuilder networkId(@Nonnull String networkId) {
            this.networkId = networkId;
            return this;
        }

        public BlockStoreFDBConfig.BlockStoreFDBConfigBuilder transactionBatchSize(Integer transactionBatchSize) {
            this.transactionBatchSize = transactionBatchSize;
            return this;
        }

        public BlockStoreFDBConfig build() {
            return new BlockStoreFDBConfig(runtimeConfig, clusterFile, apiVersion, networkId, transactionBatchSize);
        }
    }
}
