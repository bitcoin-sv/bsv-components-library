package com.nchain.jcl.base.tools.streams;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-08 13:02
 *
 * Implementation of and Endpoint that represents both the Source and the Destination of a Stream.
 * // TODO: Pending to Test and extend Documentation
 */
public interface StreamEndpoint<T> extends Stream<T> {
    void init();
    InputStreamSource<T> source();
    OutputStreamDestination<T> destination();
}
