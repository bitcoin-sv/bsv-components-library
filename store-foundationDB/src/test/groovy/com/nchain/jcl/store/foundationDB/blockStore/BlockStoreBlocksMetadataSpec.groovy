package com.nchain.jcl.store.foundationDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStoreBlockMetadataSpecBase
import com.nchain.jcl.store.blockStore.BlockStoreBlocksSpecBase
import com.nchain.jcl.store.blockStore.metadata.Metadata
import com.nchain.jcl.store.foundationDB.FDBTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory

/**
 * Testing class for basic Scenarios for Block Metadata.
 * @see BlockStoreBlocksSpecBase
 */
class BlockStoreBlocksMetadataSpec extends BlockStoreBlockMetadataSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}
