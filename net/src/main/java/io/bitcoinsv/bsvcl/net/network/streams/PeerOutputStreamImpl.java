package io.bitcoinsv.bsvcl.net.network.streams;


import io.bitcoinsv.bsvcl.net.network.PeerAddress;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author i.fernandezs@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An implementation of a PeerOutputStream, that can be linked to other PeerOutputStreams, forming a chain
 * of PeerOutputStreams where each one of them implements a "transformation" function over the data, so we
 * can actually have a chain of OutputStreams, transforming the data along the way.
 * <p>
 * for example, we can define a chain like this:
 * - An peerOutputStream1, which takes instances of a "Student" class, convert them to String and send the String
 * - An peerOutputStream2, which takes Strings and convert them into Integers before sending
 * - An peerOutputStream3, which takes Strings and convert them into bytes before sending
 * <p>
 * This chain can be represented by this:
 * <p>
 * (our main program) >> (Student<"John">) >> [outputStream1] >> "john" >> [outputStream2] >> 5 >> [outputStream3] >> 0101
 * <p>
 * So this class represents a PeerOutputStream that can take some data, run transformations on it, and sending it
 * to the next PeerOutputStream in line. The "transform" method will implement the transformation function, and will have
 * to be overwritten by the extending classes.
 * <p>
 * - param O (OUTPUT): The data that we send to this Stream
 * - param R (RESULT): The data that this Stream sends to the next OutputStream in line AFTER the transformation.
 * <p>
 * The transformation function over the data can be executed in blocking mode or in no-blocking mode, running
 * in a different Thread, depending on the ExecutorService passed to the constructor.
 */
public abstract class PeerOutputStreamImpl<O, R> implements PeerOutputStream<O> {

    protected PeerAddress peerAddress;
    protected PeerOutputStream<R> destination;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final IStreamHolder<O> streamHolder = new StreamHolder();

    /**
     * Constructor.
     *
     * @param peerAddress The peer the output stream is writing too
     * @param destination The Output Stream that is linked to this OutputStream.
     */
    protected PeerOutputStreamImpl(PeerAddress peerAddress, PeerOutputStream<R> destination) {
        this.peerAddress = peerAddress;
        this.destination = destination;
    }

    protected PeerOutputStreamImpl(PeerOutputStream<R> destination) {
        this(destination.getPeerAddress(), destination);
    }

    @Override
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }

    @Override
    public StreamState getState() {
        return null;
    }

    @Override
    public void send(O data) {
        stream(streamHolder -> streamHolder.send(data));
    }

    @Override
    public void close(StreamCloseEvent event) {
        if (destination != null) destination.close(event);
    }

    /**
     * This method implements the Transformation over the data, before is sent to the next OutputStream in line
     */
    public abstract List<R> transform(O data);

    @Override
    public void stream(Consumer<IStreamHolder<O>> streamer) {
        writeLock.lock();
        streamer.accept(this.streamHolder);
        writeLock.unlock();
    }

    private final class StreamHolder implements IStreamHolder<O> {

        @Override
        public void send(O data) {
            if (destination == null) {
                return;
            }

            final List<R> dataTransformed = transform(data);

            if (dataTransformed == null || dataTransformed.isEmpty()) {
                return;
            }

            destination.stream(s -> dataTransformed.forEach(s::send));
        }

        @Override
        public void close() {
            // we do nothing...
        }
    }
}