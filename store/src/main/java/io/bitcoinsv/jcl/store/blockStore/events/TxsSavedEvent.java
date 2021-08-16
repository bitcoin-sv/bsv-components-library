/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockStore.events;


import io.bitcoinj.core.Sha256Hash;

import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when several TXs are Stored
 */
public final class TxsSavedEvent extends BlockStoreEvent {
    private final List<Sha256Hash> txHashes;

    public TxsSavedEvent(List<Sha256Hash> txHashes) {
        this.txHashes = new ArrayList<>(txHashes);
    }

    public List<Sha256Hash> getTxHashes() {
        return this.txHashes;
    }

    @Override
    public String toString() {
        return "TxsSavedEvent(txHashes=" + this.getTxHashes() + ")";
    }
}
