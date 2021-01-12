package com.nchain.jcl.store.keyValue.blockStore;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Configuration interface with the parameters that a BlockStore needs in order to work. Any "BlockStore" instance
 * will need an instance of this Interface in order to work.
 */

public interface BlockStoreKeyValueConfig {

    /**
     * Returns the maximum Number of Items that can be processed in each Transaction. Some DB-specific implementations
     * might have limits with that, so in order to make a generic-implementation, we are providing first-calss support
     * from the very beginning, making this a configuration feature. If the "BlockStore" implementations tries to
     * perform an operation that imply more items than this number, then the work will have to be broken down into
     * smaller sub-ist that can be processed separately (in different DB transactions)
     */
    int getTransactionBatchSize();

    /**
     * Returns the Network ID representing the Chain stored.
     */
    String getNetworkId();
}
