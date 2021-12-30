package io.bitcoinsv.jcl.store.levelDB.blockStore


import io.bitcoinsv.jcl.store.blockStore.BlockStoreLinkSpecBase
import io.bitcoinsv.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata

/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
class BlockStoreLinkSpec extends BlockStoreLinkSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}