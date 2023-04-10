package io.bitcoinsv.jcl.tools.bigObjects.receivers;


import io.bitcoinsv.jcl.tools.bigObjects.receivers.events.BigObjectHeaderReceivedEvent;
import io.bitcoinsv.jcl.tools.bigObjects.receivers.events.BigObjectItemsReceivedEvent;
import io.bitcoinsv.jcl.tools.bigObjects.receivers.events.BigObjectReceivedEvent;
import io.bitcoinsv.jcl.tools.bigObjects.receivers.events.BigObjectSourceChangedEvent;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.events.EventStreamer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class implementes a wrapper over all the different Event Streamer that can be published by a
 * "BigObjectHeaderPlusReceiver":
 * - A EventStreamer that publishes Events every time a complete Object has been received
 * - A EventStreamer that publishes Events every time a CHUNK of items of an Object has been received
 * - A Event Streamer that publishes Events every time the SORUCE of some object has changed.
 */
public class BigObjectHeaderPlusReceiverEventStreamer {

    private EventBus eventBus;

    public EventStreamer<BigObjectReceivedEvent> OBJECT_RECEIVED;
    public EventStreamer<BigObjectHeaderReceivedEvent> HEADER_RECEIVED;
    public EventStreamer<BigObjectItemsReceivedEvent> ITEMS_RECEIVED;
    public EventStreamer<BigObjectSourceChangedEvent> SOURCE_CHANGED;

    public EventStreamer<BigObjectReceivedEvent> OBJECT_RECEIVED(int numThreads) {
        return new EventStreamer<>(eventBus, BigObjectReceivedEvent.class, numThreads);
    }

    public EventStreamer<BigObjectHeaderReceivedEvent> HEADER_RECEIVED(int numThreads) {
        return new EventStreamer<>(eventBus, BigObjectHeaderReceivedEvent.class, numThreads);
    }

    public EventStreamer<BigObjectItemsReceivedEvent> ITEMS_RECEIVED(int numThreads) {
        return new EventStreamer<>(eventBus, BigObjectItemsReceivedEvent.class, numThreads);
    }

    public EventStreamer<BigObjectSourceChangedEvent> SOURCE_CHANGED(int numThreads) {
        return new EventStreamer<>(eventBus, BigObjectSourceChangedEvent.class, numThreads);
    }

    public BigObjectHeaderPlusReceiverEventStreamer(EventBus eventBus) {
        this.eventBus = eventBus;
        this.OBJECT_RECEIVED    = new EventStreamer<>(eventBus, BigObjectReceivedEvent.class);
        this.HEADER_RECEIVED    = new EventStreamer<>(eventBus, BigObjectHeaderReceivedEvent.class);
        this.ITEMS_RECEIVED     = new EventStreamer<>(eventBus, BigObjectItemsReceivedEvent.class);
        this.SOURCE_CHANGED     = new EventStreamer<>(eventBus, BigObjectSourceChangedEvent.class);
    }

}