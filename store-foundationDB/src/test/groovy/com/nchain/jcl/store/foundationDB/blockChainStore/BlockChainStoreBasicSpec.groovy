package com.nchain.jcl.store.foundationDB.blockChainStore

import com.nchain.jcl.base.domain.api.base.BlockHeader
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockChainStore.BlockChainStoreBasicSpecBase
import com.nchain.jcl.store.foundationDB.DockerTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory

import java.time.Duration

/**
 * Basic Tests to verify that the Chain info is saved and is consistent after saving or removing Blocks.
 */
class BlockChainStoreBasicSpec extends BlockChainStoreBasicSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { DockerTestUtils.startDocker()}
    def cleanupSpec()   { DockerTestUtils.stopDocker()}

    @Override
    BlockChainStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents,
                                BlockHeader genesisBlock,
                                Duration publishStateFrequency,
                                Duration automaticPrunningFrequency,
                                Integer prunningHeightDifference) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, genesisBlock,
                publishStateFrequency, automaticPrunningFrequency, prunningHeightDifference)
    }
}