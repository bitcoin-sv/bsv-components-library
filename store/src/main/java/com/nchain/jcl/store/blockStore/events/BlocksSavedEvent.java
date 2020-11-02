package com.nchain.jcl.store.blockStore.events;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;

import java.util.List;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when several Blocks are sucessfully stored.
 */
@Builder
public class BlocksSavedEvent extends BlockStoreEvent {
    private List<Sha256Wrapper> blockHashes;
}
