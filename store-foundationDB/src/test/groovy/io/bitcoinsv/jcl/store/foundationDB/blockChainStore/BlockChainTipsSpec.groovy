/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.foundationDB.blockChainStore

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore
import io.bitcoinsv.jcl.store.foundationDB.FDBTestUtils
import io.bitcoinsv.jcl.store.foundationDB.StoreFactory
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.jcl.store.blockChainStore.BlockChainTipsSpecBase

import java.time.Duration

/**
 * Test scenarios involving a Fork and Prune operations
 */
class BlockChainTipsSpec extends BlockChainTipsSpecBase {

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