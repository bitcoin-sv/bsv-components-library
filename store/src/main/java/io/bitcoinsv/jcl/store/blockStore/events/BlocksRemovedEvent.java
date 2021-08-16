package io.bitcoinsv.jcl.store.blockStore.events;


import io.bitcoinj.core.Sha256Hash;

import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when several Blocks are removed form the Storage.
 */
public final class BlocksRemovedEvent extends BlockStoreEvent {
    private final List<Sha256Hash> blockHashes;

    public BlocksRemovedEvent(List<Sha256Hash> blockHashes) {
        this.blockHashes = new ArrayList<>(blockHashes);
    }

    public List<Sha256Hash> getBlockHashes() {
        return this.blockHashes;
    }

    @Override
    public String toString() {
        return "BlocksRemovedEvent(blockHashes=" + this.getBlockHashes() + ")";
    }
}
