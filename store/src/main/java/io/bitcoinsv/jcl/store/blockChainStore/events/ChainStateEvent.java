/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore.events;

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStoreState;
import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreEvent;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-10-27
 */
public final class ChainStateEvent extends BlockStoreEvent {
    private final BlockChainStoreState state;

    public ChainStateEvent(BlockChainStoreState state) {
        this.state = state;
    }

    public BlockChainStoreState getState() {
        return this.state;
    }

    @Override
    public String toString() {
        return "ChainStateEvent(state=" + this.getState() + ")";
    }
}
