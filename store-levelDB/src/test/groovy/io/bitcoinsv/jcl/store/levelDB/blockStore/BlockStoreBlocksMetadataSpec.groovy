package io.bitcoinsv.jcl.store.levelDB.blockStore


import io.bitcoinsv.jcl.store.blockStore.BlockStoreBlockMetadataSpecBase
import io.bitcoinsv.jcl.store.blockStore.BlockStoreBlocksSpecBase
import io.bitcoinsv.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata

/**
 * Testing class for basic Scenarios for Block Metadata.
 * @see BlockStoreBlocksSpecBase
 */
class BlockStoreBlocksMetadataSpec extends BlockStoreBlockMetadataSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}