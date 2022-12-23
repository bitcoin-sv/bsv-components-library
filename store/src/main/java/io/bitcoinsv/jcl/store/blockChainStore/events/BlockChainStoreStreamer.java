package io.bitcoinsv.jcl.store.blockChainStore.events;


import io.bitcoinsv.jcl.store.blockStore.events.BlockStoreStreamer;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.events.EventStreamer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A BlockChainStorage Streamer adds new Chain-related Streams over the ones provided by the BlockStoreStreamer.
 */
public class BlockChainStoreStreamer extends BlockStoreStreamer {

    public final EventStreamer<ChainForkEvent> FORKS;
    public final EventStreamer<ChainPruneEvent> PRUNINGS;
    public final EventStreamer<ChainPruneAlertEvent> PRUNING_ALERTS;
    public final EventStreamer<ChainStateEvent> STATE;

    /** Constructor */
    public BlockChainStoreStreamer(EventBus eventBus) {
        super(eventBus);
        this.FORKS = new EventStreamer<>(eventBus, ChainForkEvent.class);
        this.PRUNINGS = new EventStreamer<>(eventBus, ChainPruneEvent.class);
        this.PRUNING_ALERTS = new EventStreamer<>(eventBus, ChainPruneAlertEvent.class);
        this.STATE = new EventStreamer<>(eventBus, ChainStateEvent.class);
    }
}