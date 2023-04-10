package io.bitcoinsv.jcl.net.network.streams;


import io.bitcoinsv.jcl.net.network.PeerAddress;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * An implementation of an PeerInputStream, that can be linked to other PeerInputStreams, forming a chain
 * of PeerInputStreams where each one of them implements a "transformation" function over the data, so we
 * can actually have a chain of PeerInputStreams, transforming the data along the way.
 * <p>
 * for example, we can define a chain like this:
 * - An peerInputStream1, which takes bytes and convert them into Integers before returning them
 * - An peerInputStream2, which takes Integers and returns String
 * - An peerInputStream3, which takes Strings and returns instances of a class "Student".
 * <p>
 * This chain can be represented by this:
 * <p>
 * (our main program) << (Student<"John">) << [inputStream3] << "john" << [inputStream2] << 5 << [inputStream1] << 0101
 * <p>
 * So this class represents a PeerInputStream that can receive some data, run transformations on it, and return
 * a different data type. The "transform" method will implement the transformation function, and will have
 * to be overwritten by the extending classes.
 * <p>
 * - param I (INPUT):  The data that this Stream receives from its source (next InputStream in line)
 * - param R (RESULT): The data received from the source,  after the transformation. This is the data that the "client"
 * of this InputStream will be notified of.
 * <p>
 * The transformation function over the data can be executed in blocking mode or in no-blocking mode (running
 * in a different Thread), depending on the ExecutorService passed to the constructor.
 */
public abstract class PeerInputStreamImpl<I, R> implements PeerInputStream<R> {

    protected final Set<Consumer<R>> onDataListeners = new HashSet<>();
    protected final Set<Consumer<StreamCloseEvent>> onCloseListeners = new HashSet<>();
    protected final Set<Consumer<Throwable>> onErrorListeners = new HashSet<>();

    protected final PeerAddress peerAddress;
    protected final PeerInputStream<I> source;

    /**
     * Constructor.
     *
     * @param source The input Stream that is linked to this InputStream.
     */
    protected PeerInputStreamImpl(PeerAddress peerAddress, PeerInputStream<I> source) {
        this.peerAddress = peerAddress;
        this.source = source;

        linkSource(source);
    }

    protected PeerInputStreamImpl(PeerInputStream<I> source) {
        this(source.getPeerAddress(), source);
    }

    protected void linkSource(PeerInputStream<I> source) {
        if (source == null) {
            return;
        }

        source.onData(this::receiveAndTransform);
        source.onClose(event -> onCloseListeners.forEach(streamCloseEventConsumer -> streamCloseEventConsumer.accept(event)));
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
    public void onData(Consumer<R> eventHandler) {
        onDataListeners.add(eventHandler);
    }

    @Override
    public void onClose(Consumer<StreamCloseEvent> eventHandler) {
        onCloseListeners.add(eventHandler);
    }

    @Override
    public void onError(Consumer<Throwable> eventHandler) {
        onErrorListeners.add(eventHandler);
    }

    @Override
    public void close(StreamCloseEvent event) {
        onCloseListeners.forEach(streamCloseEventConsumer -> streamCloseEventConsumer.accept(event));
    }

    @Override
    public void expectedMessageSize(long messageSize) {
    }

    protected void receiveAndTransform(I dataEvent) {
        try {
            ofNullable(transform(dataEvent))
                    .ifPresent(streamDataEvents -> streamDataEvents.forEach(rStreamDataEvent ->
                            onDataListeners.forEach(streamCloseEventConsumer -> streamCloseEventConsumer.accept(rStreamDataEvent)))
                    );
        } catch (Exception e) {
            onErrorListeners.forEach(streamCloseEventConsumer -> streamCloseEventConsumer.accept(e));
        }
    }

    /**
     * This method implements the Transformation over the data, before is returned to the "client"
     */
    public abstract List<R> transform(I dataEvent);

}