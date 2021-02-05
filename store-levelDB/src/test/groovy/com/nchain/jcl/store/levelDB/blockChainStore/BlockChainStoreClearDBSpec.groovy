package com.nchain.jcl.store.levelDB.blockChainStore


import com.nchain.jcl.store.blockChainStore.BlockChainClearDBSpecBase
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockStore.BlockStoreBlocksSpecBase
import com.nchain.jcl.store.levelDB.StoreFactory
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly

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