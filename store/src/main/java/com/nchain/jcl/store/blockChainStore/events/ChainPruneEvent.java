package com.nchain.jcl.store.blockChainStore.events;

import com.nchain.jcl.store.blockStore.events.BlockStoreEvent;
import io.bitcoinj.core.Sha256Hash;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-10-27
 */
@Builder
@Value
@EqualsAndHashCode(callSuper = false)
public class ChainPruneEvent extends BlockStoreEvent {
    private Sha256Hash tipForkHash;
    private Sha256Hash parentForkHash;
    private long numBlocksPruned;
}
