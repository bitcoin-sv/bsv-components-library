package com.nchain.jcl.store.levelDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStoreTxsSpecBase
import com.nchain.jcl.store.blockStore.metadata.Metadata
import com.nchain.jcl.store.levelDB.StoreFactory

/**
 * Testig class with Scenarios specific for Txs
 */
/**
 * Testing class with Scenarios specific for Txs.
 * @see com.nchain.jcl.store.blockStore.BlockStoreTxsSpecBase
 */
class BlockStoreTxsSpec extends BlockStoreTxsSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}
