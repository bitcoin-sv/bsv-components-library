package com.nchain.jcl.store.foundationDB.blockChainStore

import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockChainStore.BlockChainStoreBasicSpecBase
import com.nchain.jcl.store.foundationDB.FDBTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly

import java.time.Duration

/**
 * Basic Tests to verify that the Chain info is saved and is consistent after saving or removing Blocks.
 */
class BlockChainStoreBasicSpec extends BlockChainStoreBasicSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockChainStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents,
                                HeaderReadOnly genesisBlock,
                                Duration publishStateFrequency,
                                Duration forkPrunningFrequency,
                                Integer forkPrunningHeightDiff,
                                Duration orphanPrunningFrequency,
                                Duration orphanPrunningBlockAge) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, genesisBlock,
                publishStateFrequency, forkPrunningFrequency, forkPrunningHeightDiff,
                orphanPrunningFrequency, orphanPrunningBlockAge)
    }
}