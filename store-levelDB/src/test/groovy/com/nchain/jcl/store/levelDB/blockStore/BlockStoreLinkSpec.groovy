package com.nchain.jcl.store.levelDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStoreLinkSpecBase
import com.nchain.jcl.store.levelDB.StoreFactory

/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
class BlockStoreLinkSpec extends BlockStoreLinkSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }
}