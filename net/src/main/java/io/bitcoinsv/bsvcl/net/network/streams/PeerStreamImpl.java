package io.bitcoinsv.bsvcl.net.network.streams;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Base Implementtion of a PeerStream. Futher implementations should extend this.
 */
public abstract class PeerStreamImpl<S,T> implements PeerStream<S> {

    protected PeerAddress peerAddress;
    protected PeerStream<T> streamOrigin;

    protected PeerInputStream<S> inputStream;
    protected PeerOutputStream<S> outputStream;


    public PeerStreamImpl(PeerStream<T> streamOrigin) {
        this(streamOrigin.getPeerAddress(), streamOrigin);
    }

    public PeerStreamImpl(PeerAddress peerAddress, PeerStream<T> streamOrigin) {
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