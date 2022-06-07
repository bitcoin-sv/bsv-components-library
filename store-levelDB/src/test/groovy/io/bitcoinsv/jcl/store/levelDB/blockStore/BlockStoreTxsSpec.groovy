package io.bitcoinsv.jcl.store.levelDB.blockStore


import io.bitcoinsv.jcl.store.blockStore.BlockStoreTxsSpecBase
import io.bitcoinsv.jcl.store.levelDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata

/**
 * Testig class with Scenarios specific for Txs
 */
/**
 * Testing class with Scenarios specific for Txs.
 * @see BlockStoreTxsSpecBase
 */
class BlockStoreTxsSpec extends BlockStoreTxsSpecBase {
    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass, Class<? extends Metadata> txMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass)
    }
}
