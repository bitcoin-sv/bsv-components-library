/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Base Implementtion of a PeerStream. Futher implementations should extend this.
 */
public abstract class PeerStreamImpl<S,T> implements PeerStream<S> {

    protected ExecutorService executor;
    protected PeerAddress peerAddress;
    protected PeerStream<T> streamOrigin;

    protected PeerInputStream<S> inputStream;
    protected PeerOutputStream<S> outputStream;


    public PeerStreamImpl(ExecutorService executor, PeerStream<T> streamOrigin) {
        this(streamOrigin.getPeerAddress(), executor, streamOrigin);
    }

    public PeerStreamImpl(PeerAddress peerAddress, ExecutorService executor, PeerStream<T> streamOrigin) {
        this.executor = executor;
        this.peerAddress = peerAddress;
        this.streamOrigin = streamOrigin;
    }

    @Override
    public PeerInputStream<S> input() {
        return inputStream;
    }

    @Override
    public PeerOutputStream<S> output() {
        return outputStream;
    }

    public void init() {
        this.inputStream = buildInputStream();
        this.outputStream = buildOutputStream();
    }

    // To be overwritten by extending classes
    public abstract PeerInputStream<S> buildInputStream();
    public abstract PeerOutputStream<S> buildOutputStream();
}
