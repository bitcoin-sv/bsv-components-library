package io.bitcoinsv.jcl.store.levelDB.blockStore

import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.BlockStoreBlockMetadataSpecBase
import io.bitcoinsv.jcl.store.blockStore.BlockStoreBlocksSpecBase
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import io.bitcoinsv.jcl.store.levelDB.StoreFactory

/**
 * Testing class for basic Scenarios for Block Metadata.
 * @see BlockStoreBlocksSpecBase
 */
class BlockStoreTxsMetadataSpec extends BlockStoreBlockMetadataSpecBase {
    @Override
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass, Class<? extends Metadata> txMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass)
    }
}