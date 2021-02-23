package com.nchain.jcl.store.blockStore.events;

import io.bitcoinj.core.Sha256Hash;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 23/02/2021
 *
 * An event triggered when an invalid block has been detected
 */
public final class InvalidBlockEvent extends BlockStoreEvent {
    private Sha256Hash blockHash;

    public InvalidBlockEvent(Sha256Hash blockHash) {
        this.blockHash = blockHash;
    }

    public Sha256Hash getBlockHash() {
        return this.blockHash;
    }

    @Override
    public String toString() {
        return "BlocksSavedEvent(blockHash=" + this.getBlockHash() + ")";
    }
}
