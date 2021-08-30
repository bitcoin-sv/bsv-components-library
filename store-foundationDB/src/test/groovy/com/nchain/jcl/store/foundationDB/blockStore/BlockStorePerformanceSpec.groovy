package com.nchain.jcl.store.foundationDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStorePerformanceSpecBase
import com.nchain.jcl.store.blockStore.metadata.Metadata
import com.nchain.jcl.store.foundationDB.FDBTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory
import spock.lang.Ignore


/**
 * Performance Testing class for the BlockStore
 */
// Test Ignored. If you want to run this Test, set up a local FDB or configure FDBTestUtils.useDocker to use the
// Docker image provided instead (not fully tested at the moment)
@Ignore
class BlockStorePerformanceSpec extends BlockStorePerformanceSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}
