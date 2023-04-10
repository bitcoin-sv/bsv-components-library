package io.bitcoinsv.jcl.store.blockChainStore.events;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreEvent;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This Event represents a Fork Prunning that has been detected but NOT YET executed. It inlucdes the Tip of the
 * fork (highest Block n that branch) and the Lsit of Blocks that will  be removed IF the Fork is Pruned.
 */
public final class ChainPruneAlertEvent extends BlockStoreEvent {
    private Sha256Hash tipForkHash;
    private List<Sha256Hash> blocksRemovedIfDone;

    public ChainPruneAlertEvent(Sha256Hash tipForkHash, List<Sha256Hash> blocksRemovedIfDone) {
        this.tipForkHash = tipForkHash;
        this.blocksRemovedIfDone = blocksRemovedIfDone;
    }

    public Sha256Hash getTipForkHash() {
        return tipForkHash;
    }

    public List<Sha256Hash> getBlocksRemovedIfDone() {
        return blocksRemovedIfDone;
    }

    @Override
    public String toString() {
        return "ChainPruneReqEvent[tipForkHash = " + this.tipForkHash + "]";
    }
}