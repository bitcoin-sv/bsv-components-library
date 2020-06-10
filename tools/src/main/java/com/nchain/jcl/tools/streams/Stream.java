package com.nchain.jcl.tools.streams;



/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-08 11:33
 *
 * A Stream is an abstraction that allows us to send data/Events to a Destination, and receive
 * data/events from a Source
 * // TODO: Pending to Test and extend Documentation
 */
public interface Stream<T> {
    /** Returns the InputStream that allows us to react to data/events received */
    InputStream<T> input();
    /** Returns the OutputStream that allows us to send data/events */
    OutputStream<T> output();
}
