package com.nchain.jcl.base.tools.streams;

import lombok.AllArgsConstructor;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * // TODO: Pending to Test and extend Documentation
 */
@AllArgsConstructor
public abstract class StreamImpl<S,T> implements Stream<S> {

    protected ExecutorService executor;
    protected Stream<T> streamOrigin;

    protected InputStream<S> inputStream;
    protected OutputStream<S> outputStream;

    public StreamImpl(ExecutorService executor, Stream<T> streamOrigin) {
        this.executor = executor;
        this.streamOrigin = streamOrigin;
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
