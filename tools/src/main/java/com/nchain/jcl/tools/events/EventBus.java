package com.nchain.jcl.tools.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A simple in-Memory Publish/Subscribe EventBus Implementation:
 * You subscribe for an specific Event Type, providing a piece of Code (Consumer/Event Handler) that will be executed
 * (notified) whenever an Event of that type is published to the Bus. The notification process will perform differently,
 * depending on the "ExecutorService" instance passed during creation:
 *
 * - If not provided, the notification is BLOCKING.
 * - If provided, the notification is NO-BLOCKING, running in a Multithread environment provided by the executor
 */
public class EventBus {

    // For each Event Type, we store the list of Consumers/Event Handlers that will get run/notified
    private Map<Class<? extends Event>, List<Consumer<? extends Event>>> eventHandlers = new ConcurrentHashMap<>();

    // An executorService for notifying the Subscribers asynchronously
    private final ExecutorService executor;


    private EventBus(ExecutorService executor) {
        this.executor = executor;
    }

    public static EventBusBuilder builder() {
        return new EventBusBuilder();
    }

    private void runAndIgnoreException(Consumer eventHandler, Event event) {
        Runnable task = () -> {try {eventHandler.accept(event);} catch (Exception e) {e.printStackTrace();}};
        if (executor == null) task.run();   // Synchronously
        else executor.submit(task);         // Asynchronously
    }

    public void subscribe(Class<? extends Event> eventClass, Consumer<? extends Event> eventHandler) {
        List<Consumer<? extends Event>> consumers = new ArrayList<>();
        consumers.add(eventHandler);
        eventHandlers.merge(eventClass, consumers, (w, prev) -> {prev.addAll(w); return prev;});
    }

    public void publish(Event event) {
        for (Class eventClass : eventHandlers.keySet()) {
            if (eventClass.isInstance(event))
                eventHandlers.get(eventClass).forEach(handler -> runAndIgnoreException(handler, event));
        }
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Builder class.
     */
    public static class EventBusBuilder {
        private ExecutorService executor;

        EventBusBuilder() {}
        public EventBus.EventBusBuilder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }
        public EventBus build() { return new EventBus(executor); }
    }
}
