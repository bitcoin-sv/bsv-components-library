package io.bitcoinsv.jcl.net.unit.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.streams.*;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This is an implementation of an InputStreamSource.
 * It also implements the InputStream. This means that this class can be used as an "inputStream", and can be linked
 * to another InputStream forming a chain that will end/start with this InputStreamSource.
 *
 * - param T: Data type produced by this SOURCE.
 */
public class PeerStreamInOutSimulator<T> implements PeerInputStream<T>, PeerOutputStream<T> {

    protected final Set<Consumer<T>> onData = new HashSet<>();
    protected final Set<Consumer<StreamCloseEvent>> onClose = new HashSet<>();
    protected final Set<Consumer<Throwable>> onError = new HashSet<>();

    private PeerAddress peerAddress;

    public PeerStreamInOutSimulator(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    @Override
    public StreamState getState() {
        return null;
    }
    @Override
    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }
    @Override
    public void onData(Consumer<T> eventHandler) {
        onData.add(eventHandler);
    }
    @Override
    public void onClose(Consumer<StreamCloseEvent> eventHandler) {
        onClose.add(eventHandler);
    }
    @Override
    public void onError(Consumer<Throwable> eventHandler) {
        onError.add(eventHandler);
    }
    @Override
    public void send(T event) {
        onData.forEach(streamCloseEventConsumer -> streamCloseEventConsumer.accept(event));
    }
    @Override
    public void close(StreamCloseEvent event) {
        onClose.forEach(streamCloseEventConsumer -> streamCloseEventConsumer.accept(event));
    }

    @Override
    public void stream(Consumer<IStreamHolder<T>> streamer) {
        throw new UnsupportedOperationException("Streaming is unsupported!");
    }

    @Override
    public void expectedMessageSize(long size) {
        // nothing
    }
}