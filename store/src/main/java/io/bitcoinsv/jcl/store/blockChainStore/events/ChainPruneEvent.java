package io.bitcoinsv.jcl.store.blockChainStore.events;

import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreEvent;
import io.bitcoinj.core.Sha256Hash;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-10-27
 */
public final class ChainPruneEvent extends BlockStoreEvent {
    private final Sha256Hash tipForkHash;
    private final Sha256Hash parentForkHash;
    private final List<Sha256Hash> blocksPruned;

    public ChainPruneEvent(Sha256Hash tipForkHash, Sha256Hash parentForkHash, List<Sha256Hash> blocksPruned) {
        this.tipForkHash = tipForkHash;
        this.parentForkHash = parentForkHash;
        this.blocksPruned = blocksPruned;
    }

    public Sha256Hash getTipForkHash()          { return this.tipForkHash; }
    public Sha256Hash getParentForkHash()       { return this.parentForkHash; }
    public List<Sha256Hash> getBlocksPruned()   { return this.blocksPruned;}
    public long getNumBlocksPruned()            { return this.blocksPruned.size(); }

    public String toString() {
        return "ChainPruneEvent(tipForkHash=" + this.getTipForkHash() + ", parentForkHash=" + this.getParentForkHash() + ", numBlocksPruned=" + this.getNumBlocksPruned() + ")";
    }
}
