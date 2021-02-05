package com.nchain.jcl.tools.streams;


import com.nchain.jcl.tools.events.EventBus;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of an InputStream, that can be linked to other InputStreams, forming a chain
 * of InputStreams where each one of them implements a "transformation" function over the data, so we
 * can actually have a chain of InputStreams, transforming the data along the way.
 *
 * for example, we can define a chain like this:
 * - An inputStream1, which takes bytes and convert them into Integers before returning them
 * - An inputStream2, which takes Integers and returns String
 * - An inputStream3, which takes Strings and returns instances of a class "Student".
 *
 * This chain can be represented by this:
 *
 * (our main program) << (Student<"John">) << [inputStream3] << "john" << [inputStream2] << 5 << [inputStream1] << 0101
 *
 * So this class represents an InputStream that can receive some data, run transformations on it, and return
 * a different data type. The "transform" method will implement the transformation function, and will have
 * to be overwritten by the extending classes.
 *
 *  - param I (INPUT):  The data that this Stream receives from its source (next InputStream in line)
 *  - param R (RESULT): The data received from the source,  after the transformation. This is the data that the "client"
 *                      of this InputStream will be notified of.
 *
 * The transformation function over the data can be executed in blocking mode or in no-blocking mode (running
 * in a different Thread), depending on the ExecutorService passed to the constructor.
 */
public abstract class InputStreamImpl<I,R> implements InputStream<R> {

    protected EventBus eventBus;
    protected InputStream<I> source;

    /**
     * Constructor.
     * @param executor  The transformation on the data is executed on blocking/non-blocking mode depending on this. If
     *                  null, the data is processed in blocking mode. If not null, the data will be processed in
     *                  a separate Thread/s. on a Single Thread, the data will be processed in the same order its
     *                  received. With more Threads, the data will be processed concurrently ad the order is not
     *                  guaranteed.
     *
     * @param source    The input Stream that is linked to this InputStream.
     */
    public InputStreamImpl(ExecutorService executor,
                           InputStream<I> source) {
        this.eventBus = EventBus.builder().executor(executor).build();
        this.source = source;
        if (source != null) linkSource(source);
    }

    protected void linkSource(InputStream<I> source) {
        source.onData(this::receiveAndTransform);
        source.onClose(event -> eventBus.publish(event));
    }

    @Override
    public void onData(Consumer<? extends StreamDataEvent<R>> eventHandler)  {
        eventBus.subscribe(StreamDataEvent.class, eventHandler);
    }

    @Override
    public void onClose(Consumer<? extends StreamCloseEvent> eventHandler) {
        eventBus.subscribe(StreamCloseEvent.class, eventHandler);
    }

    @Override
    public void onError(Consumer<? extends StreamErrorEvent> eventHandler) {
        eventBus.subscribe(StreamErrorEvent.class, eventHandler);
    }

    protected synchronized void receiveAndTransform(StreamDataEvent<I> dataEvent) {
        try {
            List<StreamDataEvent<R>> dataTransformed = transform(dataEvent);
            if (dataTransformed != null) dataTransformed.forEach(e -> eventBus.publish(e));
        } catch (Throwable e) {
            eventBus.publish(new StreamErrorEvent(e));
        }
    }

    /** This class implements the Transformation over the data, before is returned to the "client" */
    public abstract List<StreamDataEvent<R>> transform(StreamDataEvent<I> dataEvent);

}
