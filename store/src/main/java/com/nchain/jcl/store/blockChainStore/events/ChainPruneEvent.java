package com.nchain.jcl.store.blockChainStore.events;

import com.nchain.jcl.store.blockStore.events.BlockStoreEvent;
import io.bitcoinj.core.Sha256Hash;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-10-27
 */
public final class ChainPruneEvent extends BlockStoreEvent {
    private final Sha256Hash tipForkHash;
    private final Sha256Hash parentForkHash;
    private final long numBlocksPruned;

    public ChainPruneEvent(Sha256Hash tipForkHash, Sha256Hash parentForkHash, long numBlocksPruned) {
        this.tipForkHash = tipForkHash;
        this.parentForkHash = parentForkHash;
        this.numBlocksPruned = numBlocksPruned;
    }

    public Sha256Hash getTipForkHash()      { return this.tipForkHash; }
    public Sha256Hash getParentForkHash()   { return this.parentForkHash; }
    public long getNumBlocksPruned()        { return this.numBlocksPruned; }

    public String toString() {
        return "ChainPruneEvent(tipForkHash=" + this.getTipForkHash() + ", parentForkHash=" + this.getParentForkHash() + ", numBlocksPruned=" + this.getNumBlocksPruned() + ")";
    }
}
