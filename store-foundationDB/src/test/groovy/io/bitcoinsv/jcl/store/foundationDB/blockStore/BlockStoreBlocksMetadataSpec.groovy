package io.bitcoinsv.jcl.store.foundationDB.blockStore


import io.bitcoinsv.jcl.store.blockStore.BlockStoreBlocksSpecBase
import io.bitcoinsv.jcl.store.foundationDB.FDBTestUtils
import io.bitcoinsv.jcl.store.foundationDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.BlockStoreBlockMetadataSpecBase
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import spock.lang.Ignore

/**
 * Testing class for basic Scenarios for Block Metadata.
 * @see BlockStoreBlocksSpecBase
 */
// Test Ignored. If you want to run this Test, set up a local FDB or configure FDBTestUtils.useDocker to use the
// Docker image provided instead (not fully tested at the moment)
@Ignore
class BlockStoreBlocksMetadataSpec extends BlockStoreBlockMetadataSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass, Class<? extends Metadata> txMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass)
    }
}
