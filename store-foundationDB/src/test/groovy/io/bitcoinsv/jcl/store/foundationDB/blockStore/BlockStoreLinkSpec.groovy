/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.foundationDB.blockStore


import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.foundationDB.FDBTestUtils
import io.bitcoinsv.jcl.store.foundationDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStoreLinkSpecBase
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import spock.lang.Ignore


/**
 * A Test class for scenarios related to the relationship (link) between Blocks and Txs
 */
//@TODO: Test Ignored: FDB installation in docker is not stable. A local FB installation is recommended instead.
@Ignore
class BlockStoreLinkSpec extends BlockStoreLinkSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass)
    }
}
