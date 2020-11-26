package com.nchain.jcl.store.blockStore.events;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when several TXs are Removed
 */

@Value
public class TxsRemovedEvent extends BlockStoreEvent {
    private final List<Sha256Wrapper> txHashes;

    @Builder
    public TxsRemovedEvent(List<Sha256Wrapper> txHashes) {
        this.txHashes = new ArrayList<>(txHashes);
    }
}
