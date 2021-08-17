package io.bitcoinsv.jcl.net.network.streams;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.tools.events.EventBus;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * @author i.fernandezs@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a PeerOutputStream, that can be linked to other PeerOutputStreams, forming a chain
 * of PeerOutputStreams where each one of them implements a "transformation" function over the data, so we
 * can actually have a chain of OutputStreams, transforming the data along the way.
 *
 * for example, we can define a chain like this:
 * - An peerOutputStream1, which takes instances of a "Student" class, convert them to String and send the String
 * - An peerOutputStream2, which takes Strings and convert them into Integers before sending
 * - An peerOutputStream3, which takes Strings and convert them into bytes before sending
 *
 * This chain can be represented by this:
 *
 * (our main program) >> (Student<"John">) >> [outputStream1] >> "john" >> [outputStream2] >> 5 >> [outputStream3] >> 0101
 *
 * So this class represents a PeerOutputStream that can take some data, run transformations on it, and sending it
 * to the next PeerOutputStream in line. The "transform" method will implement the transformation function, and will have
 * to be overwritten by the extending classes.
 *
 *  - param O (OUTPUT): The data that we send to this Stream
 *  - param R (RESULT): The data that this Stream sends to the next OutputStream in line AFTER the transformation.
 *
 * The transformation function over the data can be executed in blocking mode or in no-blocking mode, running
 * in a different Thread, depending on the ExecutorService passed to the constructor.
 */
public abstract class PeerOutputStreamImpl<O,R> implements PeerOutputStream<O> {

    protected EventBus eventBus;
    protected PeerAddress peerAddress;
    protected PeerOutputStream<R> destination;

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
    public PeerOutputStreamImpl(PeerAddress peerAddress, ExecutorService executor, PeerOutputStream<R> destination) {
        this.eventBus = EventBus.builder().executor(executor).build();
        this.peerAddress = peerAddress;
        this.destination = destination;
        Consumer<StreamDataEvent<O>> dataConsumer = e -> receiveAndTransform(e);
        this.eventBus.subscribe(StreamDataEvent.class, dataConsumer);
    }
    public PeerOutputStreamImpl(ExecutorService executor, PeerOutputStream<R> destination) {
        this(destination.getPeerAddress(), executor, destination);
    }

    @Override
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }
    @Override
    public StreamState getState() { return null; }
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

    /** This method implements the Transformation over the data, before is sent to the next OutputStream in line */
    public abstract List<StreamDataEvent<R>> transform(StreamDataEvent<O> data);
}
