package com.nchain.jcl.tools.streams;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * // TODO: Pending to Test and extend Documentation
 */
public abstract class StreamImpl<S,T> implements Stream<S> {

    protected ExecutorService executor;
    protected Stream<T> streamOrigin;

    protected InputStream<S> inputStream;
    protected OutputStream<S> outputStream;

    public StreamImpl(ExecutorService executor, Stream<T> streamOrigin) {
        this.executor = executor;
        this.streamOrigin = streamOrigin;
    }

    public StreamImpl(ExecutorService executor, Stream<T> streamOrigin, InputStream<S> inputStream, OutputStream<S> outputStream) {
        this.executor = executor;
        this.streamOrigin = streamOrigin;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public abstract InputStream<S> buildInputStream();
    public abstract OutputStream<S> buildOutputStream();


    @Override
    public void init() {
        this.inputStream = buildInputStream();
        this.outputStream = buildOutputStream();
    }

    @Override
    public InputStream<S> input() {
        return inputStream;
    }
    @Override
    public OutputStream<S> output() {
        return outputStream;
    }

}
