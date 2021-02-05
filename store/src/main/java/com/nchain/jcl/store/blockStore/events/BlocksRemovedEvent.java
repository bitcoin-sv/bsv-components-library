package com.nchain.jcl.store.blockStore.events;


import io.bitcoinj.core.Sha256Hash;
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
    private List<Sha256Hash> blockHashes;

    @Builder
    public BlocksRemovedEvent(List<Sha256Hash> blockHashes) {
        this.blockHashes = new ArrayList<>(blockHashes);
    }
}
