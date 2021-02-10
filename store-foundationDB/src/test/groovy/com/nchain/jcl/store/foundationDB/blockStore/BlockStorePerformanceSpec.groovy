package com.nchain.jcl.store.foundationDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.common.PerformanceSpec
import com.nchain.jcl.store.foundationDB.FDBTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory

/**
 * Performance Testing class for the BlockStore
 */
class BlockStorePerformanceSpec extends PerformanceSpec {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }
}
