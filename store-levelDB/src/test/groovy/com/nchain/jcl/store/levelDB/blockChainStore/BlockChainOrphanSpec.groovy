package com.nchain.jcl.store.levelDB.blockChainStore

import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockChainStore.BlockChainStoreOrphanSpecBase
import com.nchain.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly

import java.time.Duration

/**
 * Test scenarios involving a Fork and Prune operations
 */
class BlockChainOrphanSpec extends BlockChainStoreOrphanSpecBase {
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