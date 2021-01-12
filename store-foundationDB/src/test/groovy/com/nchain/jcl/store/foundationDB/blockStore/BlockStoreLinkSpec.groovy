package com.nchain.jcl.store.foundationDB.blockStore


import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStoreLinkSpecBase
import com.nchain.jcl.store.foundationDB.DockerTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory


/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
class BlockStoreLinkSpec extends BlockStoreLinkSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { DockerTestUtils.startDocker()}
    def cleanupSpec()   { DockerTestUtils.stopDocker()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }
}
