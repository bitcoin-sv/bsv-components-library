package com.nchain.jcl.tools.events;

import com.nchain.jcl.tools.thread.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

    private static final int DEFAULT_NUM_THREADS = 1;


    // InMemory Bus that triggers the original Events:
    private EventBus eventBus;
    // The class that specifies what evens are triggered (only the ones implementing this class)
    private Class<E> eventClass;

    // Callback triggered by each even:
    private Consumer<E> eventHandler;

    // List of possible Filter that can be injected and applied before an Event is processed:
    private List<Predicate<E>> filters = new ArrayList<>();

    // Internal Queue to store the source events and the Executors that consume it:
    private BlockingQueue events = new LinkedBlockingQueue<>();

    // We have 2 Executor: One that just loops over the Queue, and antoher one that processed each event from the
    // Queue, by applying filters and calling the callback. This second execuor can be adjusted by specifying the
    // max number of threads it uses.
    private int numThreads;
    private Executor queueExecutor; // for consuming the queue:
    private Executor eventExecutor; // for trigerring the callback for each event

    public EventStreamer(EventBus eventBus, Class<E> eventClass, int numThreads) {
        this.eventBus = eventBus;
        this.eventClass = eventClass;
        this.numThreads = numThreads;

        // We configure our own executor responsible for consuming the QUEUE of events:
        String queueThreadName = "EventStreamerQueue[" + eventClass.getSimpleName() + "]";
        this.queueExecutor = ThreadUtils.getSingleThreadExecutorService(queueThreadName);
        this.queueExecutor.execute(this::processEventsQueue);

        // We configure the executor responsible for processing the Events:
        String eventThreadName = "EventStreamerProcessor[" + eventClass.getSimpleName() + "]";
        this.eventExecutor = ThreadUtils.getCachedThreadExecutorService(eventThreadName, this.numThreads);

        // Every time an event is triggered by the Source EventBus, we add it to our eventQueue:
        eventBus.subscribe(eventClass, e -> this.events.offer(e));

    }

    public EventStreamer(EventBus eventBus, Class<E> eventClass) {
        this(eventBus, eventClass, DEFAULT_NUM_THREADS);
    }

    public EventStreamer(EventBus eventBus, Class<E> eventClass, Predicate<E> filter, int numThreads) {
        this(eventBus, eventClass, numThreads);
        filters.add(filter);
    }

    public EventStreamer(EventBus eventBus, Class<E> eventClass, Predicate<E> filter) {
        this(eventBus, eventClass, filter, DEFAULT_NUM_THREADS);
    }

    public EventStreamer<E> filter(Predicate<E> filter) {
        this.filters.add(filter);
        return this;
    }

    private void processEventsQueue() {
        try {
            while (this.eventHandler == null) {Thread.sleep(50);}
            while (true) {
                // We take next event from the Queue:
                E event = (E) events.take();

                // We apply filters on it, if any:
                boolean shouldWeProcessIt = filters.isEmpty() || filters.stream().allMatch(f -> f.test(event));

                // We process the Event:
                if (shouldWeProcessIt) {
                    this.eventExecutor.execute(() -> this.eventHandler.accept(event));
                }
            } // while...
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public void forEach(Consumer<E> eventHandler) {
        // We are defining the Consumer/Handler that will be triggered for any Event.
        this.eventHandler = eventHandler;
    }
}