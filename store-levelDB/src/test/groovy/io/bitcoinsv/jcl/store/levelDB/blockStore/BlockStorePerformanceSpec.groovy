package io.bitcoinsv.jcl.store.levelDB.blockStore


import io.bitcoinsv.jcl.store.blockStore.BlockStorePerformanceSpecBase
import io.bitcoinsv.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata

/**
 * Performance Testing class for the BlockStore
 */
class BlockStorePerformanceSpec extends BlockStorePerformanceSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass, Class<? extends Metadata> txMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass)
    }
}
