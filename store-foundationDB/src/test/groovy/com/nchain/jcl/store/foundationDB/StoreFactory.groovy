package com.nchain.jcl.store.foundationDB

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDB
import com.nchain.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDBConfig
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDB
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig

import java.nio.file.Path
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
                                       Duration forkPrunningFrequency,
                                       Integer forkPrunningHeightDiff,
                                       Duration orphanPrunningFrequency,
                                       Duration orphanPrunningBlockAge) {

        BlockChainStoreFDBConfig dbConfig = BlockChainStoreFDBConfig.chainBuild()
                .clusterFile(CLUSTER_FILE)
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
