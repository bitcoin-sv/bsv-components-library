package io.bitcoinsv.bsvcl.common.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An EventStreamer offers a more conveniente way to subscribe to Events and provide a callback
 * that will be triggered when the Event occurs.
 * The EvenSttreamer can "stream" events by calling the calback function injected by the "forEach" method.
 * Some filters can also be specified in the constructor, so not all evetns are processed. The source of the
 * Events is the EventBus provided in the constructor. This class subscribe to the events triggered by that
 * event, and for each event trigger by the EventBus, a call to the callback function is made (so that event
 * is "streamed" to the client of this class).
 * In order to "disconnect" the source of the events (EventBus fed in the constructor) from the client of this
 * class, everytime an event is triggered by the source, that Event is NOT processed by our callback, instead it
 * is pushed to an internal QUEUE, where another separete Thread process it. So the Threads that trigger the
 * "original" Events and the Threads streamed by this class are different.
 */
public class EventStreamer<E extends Event> {
    // In-Memory Bus that triggers the original Events:
    private final EventBus eventBus;

    // The class that specifies what evens are triggered (only the ones implementing this class)
    private final Class<E> eventClass;

    // Callbacks triggered by each event:
    private final List<Consumer<E>> eventHandlers = new ArrayList<>();

    // List of possible Filter that can be injected and applied before an Event is processed:
    private final List<Predicate<E>> filters = new ArrayList<>();

    public EventStreamer(EventBus eventBus, Class<E> eventClass) {
        this.eventBus = eventBus;
        this.eventClass = eventClass;
    }

    public EventStreamer(EventBus eventBus, Class<E> eventClass, Predicate<E> filter) {
        this(eventBus, eventClass);
        filters.add(filter);
    }

    public EventStreamer<E> filter(Predicate<E> filter) {
        this.filters.add(filter);
        return this;
    }

    private void processEvent(E event) {
        if (this.eventHandlers.isEmpty()) {
            return;
        }

        // We apply filters on it, if any:
        boolean shouldWeProcessIt = filters.isEmpty() || filters.stream().allMatch(f -> f.test(event));

        // We process the Event:
        if (shouldWeProcessIt) {
            try {
                this.eventHandlers.forEach(handler -> handler.accept(event));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void forEach(Consumer<E> eventHandler) {
        // Sanity check:
        if (eventHandler == null) {
            return;
        }

        // We are defining the Consumer/Handler that will be triggered for any Event.
        this.eventHandlers.add(eventHandler);

        // Every time an event is triggered by the Source EventBus, we add it to our eventQueue:
        eventBus.subscribe(eventClass, this::processEvent);
    }
}