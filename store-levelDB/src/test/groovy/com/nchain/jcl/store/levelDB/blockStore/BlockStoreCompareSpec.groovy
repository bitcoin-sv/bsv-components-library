package com.nchain.jcl.store.levelDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStoreCompareSpecBase
import com.nchain.jcl.store.levelDB.StoreFactory


/**
 * Testing class for scenarios related to the blocks Comparison Scenario
 */
class BlockStoreCompareSpec extends BlockStoreCompareSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }
}

