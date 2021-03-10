package com.nchain.jcl.net.network.streams;

import com.nchain.jcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A PeerOutputStream is an abstraction that allows us to write data to a destination. It only allows us to either
 * SEND data, or CLOSE the channel. Both scenarios are represented by specific events. Instead of just sending
 * the data, we send instead an instance of StreamEventData, that will allow us to addBytes metadata (or useful data
 * like timestamp, user info, etc) in the future.
 *
 */
public interface PeerOutputStream<T> {
    PeerAddress getPeerAddress();
    StreamState getState();
    void send(StreamDataEvent<T> event);
    void close(StreamCloseEvent event);
}
