package io.bitcoinsv.jcl.store.levelDB.blockChainStore


import io.bitcoinsv.jcl.store.blockChainStore.BlockChainForkSpecBase
import io.bitcoinsv.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore

import java.time.Duration

/**
 * Test scenarios involving a Fork and Prune operations
 */
class BlockChainForkSpec extends BlockChainForkSpecBase {
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