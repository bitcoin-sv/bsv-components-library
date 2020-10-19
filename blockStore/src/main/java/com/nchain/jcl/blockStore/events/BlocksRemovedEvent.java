package com.nchain.jcl.blockStore.events;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when Block/s are removed form the Storage.
 */
public class BlocksRemovedEvent extends BlockStoreEvent {
    private List<Sha256Wrapper> blockHashes;
}
