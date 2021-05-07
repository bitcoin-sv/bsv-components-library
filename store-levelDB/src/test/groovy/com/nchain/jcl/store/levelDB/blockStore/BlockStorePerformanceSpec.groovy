package com.nchain.jcl.store.levelDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStorePerformanceSpecBase
import com.nchain.jcl.store.blockStore.metadata.Metadata
import com.nchain.jcl.store.levelDB.StoreFactory

/**
 * Performance Testing class for the BlockStore
 */
class BlockStorePerformanceSpec extends BlockStorePerformanceSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}
