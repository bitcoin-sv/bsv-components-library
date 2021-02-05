package com.nchain.jcl.tools.streams;



/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Stream is an abstraction that allows us to send data/Events to a Destination, and receive
 * data/events from a Source
 * // TODO: Pending to Test and extend Documentation
 */
public interface Stream<T> {

    void init();

    /** Returns the InputStream that allows us to react to data/events received */
    InputStream<T> input();
    /** Returns the OutputStream that allows us to send data/events */
    OutputStream<T> output();
}
