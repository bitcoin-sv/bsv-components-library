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
 * An Event triggered when several TXs are Removed
 */

@Value
@EqualsAndHashCode(callSuper = false)
public class TxsRemovedEvent extends BlockStoreEvent {
    private final List<Sha256Hash> txHashes;

    @Builder
    public TxsRemovedEvent(List<Sha256Hash> txHashes) {
        this.txHashes = new ArrayList<>(txHashes);
    }
}
