package com.nchain.jcl.blockStore.events;

import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.base.tools.events.EventStreamer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A BlockStorage Streamer provides different EventStreamers for different Events triggered by the
 * BlockStorage Component.
 */
public class BlockStoreStreamer {
    private final EventBus eventBus;
    private final EventStreamer<BlocksStoredEvent>  BLOCKS_STORED;
    private final EventStreamer<BlocksRemovedEvent> BLOCKS_REMOVED;
    private final EventStreamer<ForkEvent>          FORKS;

    /** Constructor */
    public BlockStoreStreamer(EventBus eventBus) {
       this.eventBus = eventBus;
       this.BLOCKS_STORED = new EventStreamer<>(this.eventBus, BlocksStoredEvent.class);
       this.BLOCKS_REMOVED = new EventStreamer<>(this.eventBus, BlocksRemovedEvent.class);
       this.FORKS = new EventStreamer<>(this.eventBus, ForkEvent.class);
    }
}
