package com.nchain.jcl.blockStore.events;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Block/s is sucessfully stored.
 */
@Builder
public class BlocksStoredEvent extends BlockStoreEvent {
    private List<Sha256Wrapper> blockHashes;
}
