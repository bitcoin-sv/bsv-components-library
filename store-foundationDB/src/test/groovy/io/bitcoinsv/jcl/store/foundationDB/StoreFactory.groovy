package io.bitcoinsv.jcl.store.foundationDB


import io.bitcoinsv.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDB
import io.bitcoinsv.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDBConfig
import io.bitcoinsv.jcl.store.foundationDB.blockStore.BlockStoreFDB
import io.bitcoinsv.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata

import java.time.Duration


/**
 * A factory that creates and returns instances of BlockStore and BlockChainStore interfaces
 */
class StoreFactory {
    private static final String DOCKER_CLUSTER_FILE = "installation/fdb.cluster";

    private static String getClusterFile() {
        return FDBTestUtils.useDocker? DOCKER_CLUSTER_FILE : null;
    }
    /** It creates an instance of the BlockStore interface */
    static BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }

    /** It creates an instance of the BlockStore interface, including block Metadata */
    static BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass, Class<? extends Metadata> txMetadataClass) {
        BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                .networkId(netId)
                .clusterFile(getClusterFile())
                .build()
        BlockStoreFDB blockStore = BlockStoreFDB.builder()
                .config(config)
                .triggerBlockEvents(triggerBlockEvents)
                .triggerTxEvents(triggerTxEvents)
                .blockMetadataClass(blockMetadataClass)
                .txMetadataClass(txMetadataClass)
                .build()
        return blockStore
    }


    /** It creates an Instance of te BlockChainStore interface */
    static BlockChainStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents,
                                       HeaderReadOnly genesisBlock,
                                       Duration publishStateFrequency,
                                       Duration forkPrunningFrequency,
                                       Integer forkPrunningHeightDiff,
                                       Duration orphanPrunningFrequency,
                                       Duration orphanPrunningBlockAge) {

        BlockChainStoreFDBConfig dbConfig = BlockChainStoreFDBConfig.chainBuild()
                .clusterFile(getClusterFile())
                .networkId(netId)
                .genesisBlock(genesisBlock)
                .forkPrunningHeightDifference(forkPrunningHeightDiff)
                .orphanPrunningBlockAge(orphanPrunningBlockAge)
                .build()

        BlockChainStore db = BlockChainStoreFDB.chainStoreBuilder()
                .config(dbConfig)
                .triggerBlockEvents(triggerBlockEvents)
                .triggerTxEvents(triggerTxEvents)
                .statePublishFrequency(publishStateFrequency)
                .enableAutomaticForkPrunning(forkPrunningFrequency != null)
                .forkPrunningFrequency(forkPrunningFrequency)
                .enableAutomaticOrphanPrunning(orphanPrunningFrequency != null)
                .orphanPrunningFrequency(orphanPrunningFrequency)
                .build()
        return db
    }
}
