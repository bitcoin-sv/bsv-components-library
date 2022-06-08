package io.bitcoinsv.jcl.store.foundationDB.blockStore


import io.bitcoinsv.jcl.store.foundationDB.FDBTestUtils
import io.bitcoinsv.jcl.store.foundationDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.BlockStoreLinkSpecBase
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import spock.lang.Ignore


/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
// Test Ignored. If you want to run this Test, set up a local FDB or configure FDBTestUtils.useDocker to use the
// Docker image provided instead (not fully tested at the moment)
@Ignore
class BlockStoreLinkSpec extends BlockStoreLinkSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass, Class<? extends Metadata> txMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass)
    }
}
