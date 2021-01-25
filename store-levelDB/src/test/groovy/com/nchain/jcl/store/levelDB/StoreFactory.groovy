package com.nchain.jcl.store.levelDB

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.levelDB.blockChainStore.BlockChainStoreLevelDB
import com.nchain.jcl.store.levelDB.blockChainStore.BlockChainStoreLevelDBConfig
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDB
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDBConfig

import java.nio.file.Path
import java.time.Duration

/**
 * A factory that creates and returns instances of BlockStore and BlockChainStore interfaces
 */
class StoreFactory {

    // Folder to store the LevelDB files in:
    private static final String DB_TEST_FOLDER = "testingDB";

    // Convenience method to generate a Random Folder to use as a working Folder:
    public static String buildWorkingFolder() {
        return DB_TEST_FOLDER + "/test-" + new Random().nextInt(100) + new Random().nextInt(100);
    }

    /** It creates an instance of the BlockStore interface */
    static BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        Path dbPath = Path.of(buildWorkingFolder())
        BlockStoreLevelDBConfig dbConfig = BlockStoreLevelDBConfig.builder()
                .workingFolder(dbPath)
                .networkId(netId)
                .build()
        BlockStore db = BlockStoreLevelDB.builder()
                .config(dbConfig)
                .triggerBlockEvents(triggerBlockEvents)
                .triggerTxEvents(triggerTxEvents)
                .build()
        return db
    }


    /** It creates an Instance of te BlockChainStore interface */
    static BlockChainStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents,
                                       BlockHeader genesisBlock,
                                       Duration publishStateFrequency,
                                       Duration forkPrunningFrequency,
                                       Integer forkPrunningHeightDiff,
                                       Duration orphanPrunningFrequency,
                                       Duration orphanPrunningBlockAge) {
        Path dbPath = Path.of(buildWorkingFolder())
        BlockChainStoreLevelDBConfig dbConfig = BlockChainStoreLevelDBConfig.chainBuild()
                .workingFolder(dbPath)
                .networkId(netId)
                .genesisBlock(genesisBlock)
                .forkPrunningHeightDifference(forkPrunningHeightDiff)
                .orphanPrunningBlockAge(orphanPrunningBlockAge)
                .build()

        BlockChainStore db = BlockChainStoreLevelDB.chainStoreBuilder()
                .config(dbConfig)
                .triggerBlockEvents(triggerBlockEvents)
                .triggerTxEvents(triggerTxEvents)
                .statePublishFrequency(publishStateFrequency)
                .enableAutomaticForkPrunning(forkPrunningFrequency != null)
                .forkPrunningFrequency(forkPrunningFrequency)
                .enableAutomaticOrphanPrunning(orphanPrunningFrequency != null)
                .orphanPrunningFrequency(orphanPrunningFrequency)
                .build()
        return db;
    }
}
