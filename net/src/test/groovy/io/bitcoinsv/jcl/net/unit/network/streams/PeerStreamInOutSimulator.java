/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.net.network.streams.*;

import java.util.concurrent.ExecutorService;
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

    private PeerAddress peerAddress;
    protected EventBus eventBus;

    public PeerStreamInOutSimulator(PeerAddress peerAddress, ExecutorService executor) {
        this.peerAddress = peerAddress;
        this.eventBus = EventBus.builder().executor(executor).build();
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
    public void onData(Consumer<? extends StreamDataEvent<T>> eventHandler) {
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
    @Override
    public void send(StreamDataEvent<T> event) {
        eventBus.publish(event);
    }
    @Override
    public void close(StreamCloseEvent event) {
        eventBus.publish(event);
    }
}
