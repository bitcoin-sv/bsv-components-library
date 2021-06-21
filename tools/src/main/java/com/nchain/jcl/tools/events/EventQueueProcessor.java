package com.nchain.jcl.tools.events;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class allows for processing Events in a separate Thread/s, without impacting the execution time of the main
 * Thread. It uses an internal Queue to store those Events, and that queue is being consumed in an infinite loop.
 * The consumption of each Event is performed by executing a Consumer task sthat is also fed into this class on
 * creation.
 */
public class EventQueueProcessor {

    // List of Consumers, linked to specific Event types:
    Map<Class<? extends Event>, Consumer> eventsConsumers = new ConcurrentHashMap<>();

    // Executor used to run the Consumers/Event Handlers
    private ExecutorService executor;

    /** Constructor */
    public EventQueueProcessor(ExecutorService executor) {
        this.executor = executor;
    }

    /** It adds an Event Handler/Consumer, linked to an event Type. More than on Handler can be assigned to a Type */
    public void addProcessor(Class<? extends Event> eventClass, Consumer eventConsumer) {
        eventsConsumers.put(eventClass, eventConsumer);
    }

    /** I adds a new Event to be consumed. This method returns immediately, the Event is processed in a separate Thread */
    public void addEvent(Event event) {
        try {
            executor.submit(()-> eventsConsumers.get(event.getClass()).accept(event));
        } catch (RejectedExecutionException e) {
            // Most probably, we are trying to submit tasks when this executor has been already shutdown. This
            // might happen if the order of events triggered by the EventBus is not in the right order:
            // For example, when receiving the "NetStop" event form the EventBus, Handler will probably call the
            // "stop()" method in this class, which will shutdown the executor, so no further events will be allowed.
            // But due to the inherent Multi-thread randomness, other events might still come after the "NetStop"
            // events, and those events therefore cannot be processed.
        }
    }

    /** Starts the Execution */
    public void start() {
    }

    /** Stops the execution */
    public void stop() {
        executor.shutdownNow();
    }
}
