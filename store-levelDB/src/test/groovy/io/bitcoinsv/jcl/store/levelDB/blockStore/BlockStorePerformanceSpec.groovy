package io.bitcoinsv.jcl.store.levelDB.blockStore

import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.BlockStorePerformanceSpecBase
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import io.bitcoinsv.jcl.store.levelDB.StoreFactory

/**
 * Performance Testing class for the BlockStore
 */
class BlockStorePerformanceSpec extends BlockStorePerformanceSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}
