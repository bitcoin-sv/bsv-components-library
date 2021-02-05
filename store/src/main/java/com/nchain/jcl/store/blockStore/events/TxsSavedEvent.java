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
 * An Event triggered when several TXs are Stored
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class TxsSavedEvent extends BlockStoreEvent {
    private final List<Sha256Hash> txHashes;

    @Builder
    public TxsSavedEvent(List<Sha256Hash> txHashes) {
        this.txHashes = new ArrayList<>(txHashes);
    }
}
