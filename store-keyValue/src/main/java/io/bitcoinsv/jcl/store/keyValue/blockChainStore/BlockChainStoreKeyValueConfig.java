package io.bitcoinsv.jcl.store.keyValue.blockChainStore;


import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;

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

    boolean isForkPruningAutomaticEnabled();
    boolean isForkPruningAlertEnabled();
    boolean isOrphanPruningAutomaticEnabled();
    /**
     * If the automatic Prunning is enabled, then any shorter branch which difference with the main branch is
     * equals or more than this number, will be pruned
     */
    int getForkPruningHeightDifference();

    /** Return the Frequency by which the Automatic Fork-Prunning is run */
    Duration getForkPruningFrequency();

    /** If TRUE, all the TXs linked to a Block will also be removed when the Block is pruned */
    boolean isForkPruningIncludeTxs();

    /** If Automatic Orphan Prunning is enabled, any Orphan block older than this value will be removed */
    Duration getOrphanPruningBlockAge();

    /** Return the Frequency by which the Automatic Orphan-Prunning is run */
    Duration getOrphanPruningFrequency();

}