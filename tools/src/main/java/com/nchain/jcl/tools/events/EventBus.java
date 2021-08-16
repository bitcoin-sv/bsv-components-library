package com.nchain.jcl.tools.events;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A simple in-Memory Publish/Subscribe EventBus Implementation:
 * You subscribe for an specific Event Type, providing a piece of Code (Consumer/Event Handler) that will be executed
 * (notified) whenever an Event of that type is published to the Bus.
 *
 * Yu can also specify if the Event Handler will be executed in the same Thread as the main App or in a multi-thread
 * environment. If you specify an ExecutorSerice in the constructor, that wil be used to submit task, otherwise they will
 * be run in the same Thread.
 *
 * The EventBus allows for 2-level priority Events, you can specfy what evetn are the ones with higer priority. In order
 * to enable this fetaure, you need to:
 * - Pass an additional executor (executorHighPriority) in the constructor. This will be used for the high-priority
 *   Events.
 * - Pass a Function in the constructor that will be used to decide the priority of each Event.
 *
 */
public class EventBus {

    private static Logger log = LoggerFactory.getLogger(EventBus.class);

    // For each Event Type, we store the list of Consumers/Event Handlers that will get run/notified
    private Map<Class<? extends Event>, List<Consumer<? extends Event>>> eventHandlers = new ConcurrentHashMap<>();

    // Every time an Event is published, we need to get the List of Consumers/Event Handlers linked to that Event Type,
    // and execute them in sequence. That implies tht we need to create an additional "task" that loops over those
    // Handlers and executes one after another. In order to improve performance, we are NOT doing that at the moment of
    // publishing en Event. Instead, we do that we you subscribe the Handlers. So we keep an additional structure where
    // we have already built this "wrapper" task that loop over the Handlers and executes them in sequence:

    private Map<Class<? extends Event>, Consumer> eventHandlersOptimized = new ConcurrentHashMap<>();

    // We keep track of the number of events published (ONLY FOR TESTING; TIME CONSUMING TASK)
    private Map<Class<? extends Event>, Long> numEventsPublished = new ConcurrentHashMap<>();

    // An executor for running the Handlers:
    private ExecutorService executor;


    /** Constructor */
    private EventBus(ExecutorService executor) {
        this.executor = executor;
    }

    private EventBus() {}

    /**
     * It assigns a Handler to an Event Type. More than one Handler can be linked to an Event Type, and they are all
     * executed in sequence.
     */
    public synchronized void subscribe(Class<? extends Event> eventClass, Consumer<? extends Event> eventHandler) {
        // We addBytes the handler to the list of handlers assigned to this Event Type:
        List<Consumer<? extends Event>> consumers = new ArrayList<>();
        consumers.add(eventHandler);
        eventHandlers.merge(eventClass, consumers, (w, prev) -> {prev.addAll(w); return prev;});

        // We build a "wrapper"(consumer optimized  task that executes ll the handlers linked to this Event Type in
        // sequence. This is the task that will be executed when an Event is published:

        Consumer<? extends Event> consumerOptimized = (event) -> {
            List<Consumer<? extends Event>> eventConsumers = eventHandlers.get(eventClass);
            for (Consumer consumer : eventConsumers) {
                try {consumer.accept(event);} catch (Exception e) {e.printStackTrace();}
            }
        };

        eventHandlersOptimized.put(eventClass, consumerOptimized);
    }

    /**
     * It publishes a new Event to the Bus and executes the handlers subscribed to it
     */
    public void publish(Event event) {
        // We do not do anything at all if nobody is listening to this event
        if (eventHandlersOptimized.containsKey(event.getClass())) {
            Runnable task = () -> {eventHandlersOptimized.get(event.getClass()).accept(event);};
            if (executor != null) { // Asynchronously
                try {
                    executor.submit(task);
                } catch (RejectedExecutionException e) {
                    log.error(e.getMessage(), e);
                }
            }
            else {
                task.run(); // Synchronously
            }
            numEventsPublished.merge(event.getClass(), 1L ,Long::sum);
        }

    }

    /** Returns the EVentBus Status (ONLY FOR TESTING/DEBUGGING) */
    public String getStatus() {
        String result = "";
        Iterator<Class<? extends Event>> events = eventHandlers.keySet().iterator();
        while (events.hasNext()) {
            Class eventClass = events.next();
            Long numEvents = numEventsPublished.get(eventClass) != null ? numEventsPublished.get(eventClass) : 0L;
            result += eventClass.toString() + " : " + eventHandlers.get(eventClass).size() + " handlers, " + numEvents + " events triggered \n";
        }
        return result;
    }


    public static EventBusBuilder builder() { return new EventBusBuilder(); }

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

        public EventBus build() {
            return new EventBus(executor);
        }
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }
}
