package com.nchain.jcl.store.blockStore.events;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when several Blocks are removed form the Storage.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class BlocksRemovedEvent extends BlockStoreEvent {
    private List<Sha256Wrapper> blockHashes;

    @Builder
    public BlocksRemovedEvent(List<Sha256Wrapper> blockHashes) {
        this.blockHashes = new ArrayList<>(blockHashes);
    }
}
