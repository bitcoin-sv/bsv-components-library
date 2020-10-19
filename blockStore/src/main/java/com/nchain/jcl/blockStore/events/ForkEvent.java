package com.nchain.jcl.blockStore.events;

import com.nchain.jcl.base.domain.api.base.BlockHeader;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggred when a Fork is detected: A block has been stored that represents a different chain (it's
 * previous blocks already has a "child").
 */
public class ForkEvent extends BlockStoreEvent {
    private BlockHeader block;
}
