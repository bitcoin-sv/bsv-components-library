package io.bitcoinsv.jcl.store.keyValue.blockChainStore;


import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Configuration interface with the parameters that a BlockChainStore needs in order to work.
 * Any "BlockChainStore" instance will need an instance of this Interface in order to work.
 */

public interface BlockChainStoreKeyValueConfig extends BlockStoreKeyValueConfig {

    /** Returns the Genesis Block used to initialize the DB. This block will make the ROOT of the Chain */
    HeaderReadOnly getGenesisBlock();

    /**
     * If the automatic Prunning is enabled, then any shorter branch which difference with the main branch is
     * equals or more than this number, will be pruned
     */
    int getForkPrunningHeightDifference();

    /** If TRUE, all the TXs linked to a Block will also be removed when the Block is pruned */
    boolean isForkPrunningIncludeTxs();

    /** If Automatic Orphan Prunning is enabled, any Orphan block older than this value will be removed */
    Duration getOrphanPrunningBlockAge();

}
