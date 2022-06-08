package io.bitcoinsv.jcl.store.levelDB.blockChainStore


import io.bitcoinsv.jcl.store.blockChainStore.BlockChainClearDBSpecBase
import io.bitcoinsv.jcl.store.blockStore.BlockStoreBlocksSpecBase
import io.bitcoinsv.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore

import java.time.Duration

/**
 * Testing class for basic Scenarios for Blocks.
 * @see BlockStoreBlocksSpecBase
 */
class BlockChainStoreClearDBSpec extends BlockChainClearDBSpecBase {
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