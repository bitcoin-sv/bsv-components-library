package com.nchain.jcl.base.tools.streams;

import com.nchain.jcl.base.tools.events.EventBus;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author i.fernandezs@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-03 10:37
 *
 * An implementation of an OutputStream, that can be linked to other OutputStreams, forming a chain
 * of OutputStreams where each one of them implements a "transformation" function over the data, so we
 * can actually have a chain of OutputStreams, transforming the data along the way.
 *
 * for example, we can define a chain like this:
 * - An outputStream1, which takes instances of a "Student" class, convert them to String and send the String
 * - An outputStream2, which takes Strings and convert them into Integers before sending
 * - An outputStream3, which takes Strings and convert them into bytes before sending
 *
 * This chain can be represented by this:
 *
 * (our main program) >> (Student<"John">) >> [outputStream1] >> "john" >> [outputStream2] >> 5 >> [outputStream3] >> 0101
 *
 * So this class represents an OutputStream that can take some data, run transformations on it, and sending it
 * to the next OutputStream in line. The "transform" method will implement the transformation function, and will have
 * to be overwritten by the extending classes.
 *
 *  - param O (OUTPUT): The data that we send to this Stream
 *  - param R (RESULT): The data that this Stream sends to the next OutputStream in line AFTER the transformation.
 *
 * The transformation function over the data can be executed in blocking mode or in no-blocking mode, running
 * in a different Thread, depending on the ExecutorService passed to the constructor.
 */
public abstract class OutputStreamImpl<O,R> implements OutputStream<O> {

    protected EventBus eventBus;
    protected OutputStream<R> destination;

    /**
     * Constructor.
     * @param executor  The transformation on the data is executed on blocking/non-blocking mode depending on this. If
     *                  null, the data is processed in blocking mode. If not null, the data will be processed in
     *                  a separate Thread/s. on a Single Thread, the data will be processed in the same order its
     *                  received. With more Threads the data will be processed concurrently and the order cannot be
     *                  guaranteed.
     *
     * @param destination    The Output Stream that is linked to this OutputStream.
     */
    public OutputStreamImpl(ExecutorService executor, OutputStream<R> destination) {
        this.eventBus = EventBus.builder().executor(executor).build();
        this.destination = destination;
        Consumer<StreamDataEvent<O>> dataConsumer = e -> receiveAndTransform(e);
        this.eventBus.subscribe(StreamDataEvent.class, dataConsumer);
    }
    @Override
    public void send(StreamDataEvent<O> event)  {
        eventBus.publish(event);
    }
    @Override
    public void close(StreamCloseEvent event) {
        if (destination != null) destination.close(event);
    }

    private synchronized void receiveAndTransform(StreamDataEvent<O> data) {
        if (destination != null) {
            List<StreamDataEvent<R>> dataTransformed = transform(data);
            if (dataTransformed != null) dataTransformed.forEach(e -> destination.send(e));
        }
    }

    /** This class implements the Transformation over the data, before is sent to the next OutputStream in line */
    public abstract List<StreamDataEvent<R>> transform(StreamDataEvent<O> data);
}
