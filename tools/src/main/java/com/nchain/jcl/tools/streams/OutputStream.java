package com.nchain.jcl.tools.streams;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An OutputStream is an abstraction that allows us to write data to a destination. It only allows us to either
 * SEND data, or CLOSE the channel. Both scenarios are represented by specific events. Instead of just sending
 * the data, we send instead an instance of StreamEventData, that will allow us to add metadata (or useful data
 * like timestamp, user info, etc) in the future.
 *
 * An OutputStream does NOT provide information about the DESTINATION of the data. That information can be specified
 * when creating instances that implement this class
 */
public interface OutputStream<T> {
    void send(StreamDataEvent<T> event);
    void close(StreamCloseEvent event);
}
