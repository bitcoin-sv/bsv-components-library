package io.bitcoinsv.jcl.tools.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An EventStreamer offers a more conveniente way to subscribe to Events and provide a callback
 * that will be triggered when the Event occurs.
 * The EventStreamer is linked to an EventBus (provided on creation). Then, you subscribe to an event by providing
 * the callback you want to trigger. you can also provide a "filter", which is a Predicate that will be used to
 * filter out the Events before triggering your callback.
 */
public class EventStreamer<E extends Event> {
    private EventBus eventBus;
    private Class<E> eventClass;
    private List<Predicate<E>> filters = new ArrayList<>();

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

    public void forEach(Consumer<E> eventHandler) {
        // We are defining the Consumer/Handler that will be triggered for any Event. We start
        // the initial version is the same as the one provided as parameter:
        Consumer<E> eventHandlerToSubscribe = eventHandler;

        // But if some filters have been specified, then we build another version where all those filters
        // are applied before running the consumer/Handler:

        if (filters.size() > 0) {
            eventHandlerToSubscribe = e -> {
                boolean runConsumer = true;
                for (Predicate p : filters) {
                    runConsumer = runConsumer && p.test(e);
                    if (!runConsumer) break;
                }
                if (runConsumer) eventHandler.accept(e);
            };
        }

        // And we finally subscribe the Consumer/Handler to the EventBus:
        eventBus.subscribe(eventClass, eventHandlerToSubscribe);
    }
}