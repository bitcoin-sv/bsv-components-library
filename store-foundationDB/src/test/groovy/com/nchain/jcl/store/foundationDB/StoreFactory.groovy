package com.nchain.jcl.store.foundationDB

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDB
import com.nchain.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDBConfig
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDB
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig

import java.time.Duration


/**
 * A factory that creates and returns instances of BlockStore and BlockChainStore interfaces
 */
class StoreFactory {
    private static final String CLUSTER_FILE = "installation/fdb.cluster";

    /** It creates an instance of the BlockStore interface */
    static BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                .networkId(netId)
                .clusterFile(CLUSTER_FILE)
                .build()
        BlockStoreFDB blockStore = BlockStoreFDB.builder()
                .config(config)
                .triggerBlockEvents(triggerBlockEvents)
                .triggerTxEvents(triggerTxEvents)
                .build()
        return blockStore
    }

    /** It creates an Instance of te BlockChainStore interface */
    static BlockChainStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents,
                                       BlockHeader genesisBlock,
                                       Duration publishStateFrequency,
                                       Duration automaticPrunningFrequency,
                                       Integer prunningHeightDifference) {
        BlockChainStoreFDBConfig dbConfig = BlockChainStoreFDBConfig.chainBuilder()
                .networkId(netId)
                .clusterFile(CLUSTER_FILE)
                .genesisBlock(genesisBlock)
                .build()

        BlockChainStore db = BlockChainStoreFDB.chainStoreBuilder()
                .config(dbConfig)
                .triggerBlockEvents(triggerBlockEvents)
                .triggerTxEvents(triggerTxEvents)
                .statePublishFrequency(publishStateFrequency)
                .enableAutomaticPrunning(automaticPrunningFrequency != null)
                .prunningFrequency(automaticPrunningFrequency)
                .prunningHeightDifference((prunningHeightDifference) != null ? prunningHeightDifference : BlockChainStoreFDB.PRUNNING_HEIGHT_DIFF_DEFAULT)
                .build()
        return db;
    }
}
