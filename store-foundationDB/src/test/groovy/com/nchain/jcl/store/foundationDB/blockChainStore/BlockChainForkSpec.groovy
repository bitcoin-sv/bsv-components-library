package com.nchain.jcl.store.foundationDB.blockChainStore

import com.nchain.jcl.store.blockChainStore.BlockChainForkSpecBase
import com.nchain.jcl.store.blockChainStore.BlockChainStore
import com.nchain.jcl.store.foundationDB.FDBTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import spock.lang.Ignore

import java.time.Duration

/**
 * Test scenarios involving a Fork and Prune operations
 */
// Test Ignored. If you want to run this Test, set up a local FDB or configure FDBTestUtils.useDocker to use the
// Docker image provided instead (not fully tested at the moment)
@Ignore
class BlockChainForkSpec extends BlockChainForkSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockChainStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents,
                                HeaderReadOnly genesisBlock,
                                Duration publishStateFrequency,
                                Duration forkPrunningFrequency,
                                Integer forkPrunningHeightDiff,
                                Duration orphanPrunningFrequency,
                                Duration orphanPrunningBlockAge) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, genesisBlock,
                publishStateFrequency, forkPrunningFrequency, forkPrunningHeightDiff,
                orphanPrunningFrequency, orphanPrunningBlockAge)
    }
}