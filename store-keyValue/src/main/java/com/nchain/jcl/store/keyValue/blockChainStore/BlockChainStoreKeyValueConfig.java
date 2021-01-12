package com.nchain.jcl.store.keyValue.blockChainStore;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Configuration interface with the parameters that a BlockChainStore needs in order to work.
 * Any "BlockChainStore" instance will need an instance of this Interface in order to work.
 */

public interface BlockChainStoreKeyValueConfig extends BlockStoreKeyValueConfig {

    /** Returns the Genesis Block used to initialize the DB. This block will make the ROOT of the Chain */
    BlockHeader getGenesisBlock();
}
