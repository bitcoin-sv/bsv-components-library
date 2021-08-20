/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore.events;


import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreEvent;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-10-27
 */
public final class ChainForkEvent extends BlockStoreEvent {

    private final Sha256Hash parentForkHash;
    private final Sha256Hash blockForkHash;

    public ChainForkEvent(Sha256Hash parentForkHash, Sha256Hash blockForkHash) {
        this.parentForkHash = parentForkHash;
        this.blockForkHash = blockForkHash;
    }

    public Sha256Hash getParentForkHash()   { return this.parentForkHash; }
    public Sha256Hash getBlockForkHash()    { return this.blockForkHash; }

    @Override
    public String toString() {
        return "ChainForkEvent(parentForkHash=" + this.getParentForkHash() + ", blockForkHash=" + this.getBlockForkHash() + ")";
    }
}
