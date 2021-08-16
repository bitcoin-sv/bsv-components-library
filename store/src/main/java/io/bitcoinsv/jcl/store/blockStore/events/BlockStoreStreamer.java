/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockStore.events;


import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.events.EventStreamer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A BlockStorage Streamer provides different EventStreamers for different Events triggered by the
 * BlockStorage Component.
 */
public class BlockStoreStreamer {
    private final EventBus eventBus;
    public final EventStreamer<BlocksSavedEvent> BLOCKS_SAVED;
    public final EventStreamer<BlocksRemovedEvent>  BLOCKS_REMOVED;
    public final EventStreamer<TxsSavedEvent>       TXS_SAVED;
    public final EventStreamer<TxsRemovedEvent>     TXS_REMOVED;
    public final EventStreamer<InvalidBlockEvent> INVALID_BLOCKS;

    /** Constructor */
    public BlockStoreStreamer(EventBus eventBus) {
       this.eventBus        = eventBus;
       this.BLOCKS_SAVED    = new EventStreamer<>(this.eventBus, BlocksSavedEvent.class);
       this.BLOCKS_REMOVED  = new EventStreamer<>(this.eventBus, BlocksRemovedEvent.class);
       this.TXS_SAVED       = new EventStreamer<>(this.eventBus, TxsSavedEvent.class);
       this.TXS_REMOVED     = new EventStreamer<>(this.eventBus, TxsRemovedEvent.class);
       this.INVALID_BLOCKS = new EventStreamer<>(this.eventBus, InvalidBlockEvent.class);
    }
}
