package com.nchain.jcl.tools.streams;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of and Endpoint that represents both the Source and the Destination of a Stream.
 * // TODO: Pending to Test and extend Documentation
 */
public interface StreamEndpoint<T> extends Stream<T> {
    void init();
    InputStreamSource<T> source();
    OutputStreamDestination<T> destination();
}
