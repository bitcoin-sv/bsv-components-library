package com.nchain.jcl.store.blockChainStore.events;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.store.blockStore.events.BlockStoreEvent;
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
public class ChainForkEvent extends BlockStoreEvent {
    private Sha256Wrapper parentForkHash;
    private Sha256Wrapper blockForkHash;
}
