package com.nchain.jcl.store.levelDB.blockChainStore


import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.blockChainStore.BlockChainStoreBasicSpecBase
import com.nchain.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly

import java.time.Duration

/**
 * Basic Tests to verify that the Chain info is saved and is consistent after saving or removing Blocks.
 */
class BlockChainStoreBasicSpec extends BlockChainStoreBasicSpecBase {
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