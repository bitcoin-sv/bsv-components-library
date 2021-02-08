package com.nchain.jcl.tools.streams;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of a StreamEndpoint.
 * // TODO: Pending to Test and extend Documentation
 */
public abstract class StreamEndpointImpl<T>  implements StreamEndpoint<T>, Stream<T> {
    protected ExecutorService executor;

    protected InputStreamSourceImpl<T> source;
    protected OutputStreamDestinationImpl<T> destination;


    public StreamEndpointImpl(ExecutorService executor) {
        this.executor = executor;

    }

    public StreamEndpointImpl(ExecutorService executor, InputStreamSourceImpl<T> source, OutputStreamDestinationImpl<T> destination) {
        this.executor = executor;
        this.source = source;
        this.destination = destination;
    }

    public abstract InputStreamSourceImpl<T> buildSource();
    public abstract OutputStreamDestinationImpl<T> buildDestination();

    @Override
    public void init() {
        this.source = buildSource();
        this.destination = buildDestination();
    }
    @Override
    public InputStreamSource<T> source() {
        return source;
    }
    @Override
    public OutputStreamDestination<T> destination() {
        return destination;
    }
    @Override
    public InputStream<T> input() {
        return source;
    }
    @Override
    public OutputStream<T> output() {
        return destination;
    }
}
