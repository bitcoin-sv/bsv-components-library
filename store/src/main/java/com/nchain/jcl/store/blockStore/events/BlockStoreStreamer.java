package com.nchain.jcl.store.blockStore.events;


import com.nchain.jcl.tools.events.EventBus;
import com.nchain.jcl.tools.events.EventStreamer;

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

    /** Constructor */
    public BlockStoreStreamer(EventBus eventBus) {
       this.eventBus        = eventBus;
       this.BLOCKS_SAVED    = new EventStreamer<>(this.eventBus, BlocksSavedEvent.class);
       this.BLOCKS_REMOVED  = new EventStreamer<>(this.eventBus, BlocksRemovedEvent.class);
       this.TXS_SAVED       = new EventStreamer<>(this.eventBus, TxsSavedEvent.class);
       this.TXS_REMOVED     = new EventStreamer<>(this.eventBus, TxsRemovedEvent.class);
    }
}
