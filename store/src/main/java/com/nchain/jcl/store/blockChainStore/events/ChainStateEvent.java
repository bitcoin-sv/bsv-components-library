package com.nchain.jcl.store.blockChainStore.events;

import com.nchain.jcl.store.blockChainStore.BlockChainStoreState;
import com.nchain.jcl.store.blockStore.events.BlockStoreEvent;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-10-27
 */
@Builder
@Value
public class ChainStateEvent extends BlockStoreEvent {
    private BlockChainStoreState state;
}
