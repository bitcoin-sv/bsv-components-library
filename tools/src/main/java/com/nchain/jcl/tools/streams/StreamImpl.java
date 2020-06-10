package com.nchain.jcl.tools.streams;

import lombok.AllArgsConstructor;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-08 11:34
 *
 * // TODO: Pending to Test and extend Documentation
 */
@AllArgsConstructor
public abstract class StreamImpl<S,T> implements Stream<S> {

    private InputStream<S> inputStream;
    private OutputStream<S> outputStream;

    public StreamImpl(ExecutorService executor, Stream<T> streamOrigin) {
        this.inputStream = buildInputStream(executor, streamOrigin.input());
        this.outputStream = buildOutputStream(executor, streamOrigin.output());
    }

    public abstract InputStream<S> buildInputStream(ExecutorService executor, InputStream<T> source);
    public abstract OutputStream<S> buildOutputStream(ExecutorService executor, OutputStream<T> destination);

    @Override
    public InputStream<S> input() {
        return inputStream;
    }
    @Override
    public OutputStream<S> output() {
        return outputStream;
    }

}
