package io.bitcoinsv.jcl.store.blockStore.events;



import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

import java.util.ArrayList;
import java.util.List;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when several TXs are Removed
 */

public final class TxsRemovedEvent extends BlockStoreEvent {
    private final List<Sha256Hash> txHashes;

    public TxsRemovedEvent(List<Sha256Hash> txHashes) {
        this.txHashes = new ArrayList<>(txHashes);
    }

    public List<Sha256Hash> getTxHashes() {
        return this.txHashes;
    }

    @Override
    public String toString() {
        return "TxsRemovedEvent(txHashes=" + this.getTxHashes() + ")";
    }
}
