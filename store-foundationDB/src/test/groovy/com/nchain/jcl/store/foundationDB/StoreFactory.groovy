package com.nchain.jcl.store.foundationDB

import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.metadata.Metadata
import com.nchain.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDB
import com.nchain.jcl.store.foundationDB.blockChainStore.BlockChainStoreFDBConfig
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDB
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDBConfig
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly

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
    static BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        BlockStoreFDBConfig config = BlockStoreFDBConfig.builder()
                .networkId(netId)
                .clusterFile(getClusterFile())
                .build()
        BlockStoreFDB blockStore = BlockStoreFDB.builder()
                .config(config)
                .triggerBlockEvents(triggerBlockEvents)
                .triggerTxEvents(triggerTxEvents)
                .blockMetadataClass(blockMetadataClass)
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
