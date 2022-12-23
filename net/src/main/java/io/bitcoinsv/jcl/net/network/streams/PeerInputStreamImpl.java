package io.bitcoinsv.jcl.net.network.streams;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.tools.events.EventBus;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of an PeerInputStream, that can be linked to other PeerInputStreams, forming a chain
 * of PeerInputStreams where each one of them implements a "transformation" function over the data, so we
 * can actually have a chain of PeerInputStreams, transforming the data along the way.
 *
 * for example, we can define a chain like this:
 * - An peerInputStream1, which takes bytes and convert them into Integers before returning them
 * - An peerInputStream2, which takes Integers and returns String
 * - An peerInputStream3, which takes Strings and returns instances of a class "Student".
 *
 * This chain can be represented by this:
 *
 * (our main program) << (Student<"John">) << [inputStream3] << "john" << [inputStream2] << 5 << [inputStream1] << 0101
 *
 * So this class represents a PeerInputStream that can receive some data, run transformations on it, and return
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
public abstract class PeerInputStreamImpl<I,R> implements PeerInputStream<R> {

    protected EventBus eventBus;
    protected PeerAddress peerAddress;
    protected PeerInputStream<I> source;

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
    public PeerInputStreamImpl(PeerAddress peerAddress,ExecutorService executor, PeerInputStream<I> source) {
        this.eventBus = EventBus.builder().executor(executor).build();
        this.peerAddress = peerAddress;
        this.source = source;
        if (source != null) linkSource(source);
    }

    public PeerInputStreamImpl(ExecutorService executor, PeerInputStream<I> source) {
        this(source.getPeerAddress(), executor, source);
    }

    protected void linkSource(PeerInputStream<I> source) {
        source.onData(this::receiveAndTransform);
        source.onClose(event -> eventBus.publish(event));
    }

    @Override
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }

    @Override
    public StreamState getState() { return null;}

    @Override
    public void onData(Consumer<StreamDataEvent<R>> eventHandler)  {
        eventBus.subscribe(StreamDataEvent.class, eventHandler::accept);
    }

    @Override
    public void onClose(Consumer<StreamCloseEvent> eventHandler) {
        eventBus.subscribe(StreamCloseEvent.class, eventHandler);
    }

    @Override
    public void onError(Consumer<StreamErrorEvent> eventHandler) {
        eventBus.subscribe(StreamErrorEvent.class, eventHandler);
    }

    @Override
    public void close(StreamCloseEvent event) {
        eventBus.publish(new StreamCloseEvent());
    }

    @Override
    public void expectedMessageSize(long messageSize) { };

    protected synchronized void receiveAndTransform(StreamDataEvent<I> dataEvent) {
        try {
            List<StreamDataEvent<R>> dataTransformed = transform(dataEvent);
            if (dataTransformed != null) dataTransformed.forEach(e -> eventBus.publish(e));
        } catch (Throwable e) {
            eventBus.publish(new StreamErrorEvent(e));
        }
    }

    /**
     * This method implements the Transformation over the data, before is returned to the "client"
     */
    public abstract List<StreamDataEvent<R>> transform(StreamDataEvent<I> dataEvent);

}