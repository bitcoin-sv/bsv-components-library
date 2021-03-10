package com.nchain.jcl.net.network.streams;
import com.nchain.jcl.net.network.PeerAddress;


import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An PeerInputStream is an abstraction that works as a source of Data connected to a Peer, of which
 * we do not have control. Since we cannot control when the data is sent, we can only assign callbacks/EventHandlers
 * that will get triggered when the data arrives.
 *
 * We can get notified of 2 types of Events:
 *  - When some data arrives
 *  - When the data stream is closed.
 *
 *  In both cases we can define callbacks that will be triggered on those scenarios. Each scenario is represented by
 *  an specific event, and the callback is just a Consumer of that Event.
 *  We can addBytes more than one callback/eventHandler to one event, or nothing at all.
 *
 *  An InputStream does NOT provide information about the SOURCE of the data. That information can be specified
 *  when creating instances that implement this class
 *
 */
public interface PeerInputStream<T> {
    PeerAddress getPeerAddress();
    StreamState getState();
    void onData(Consumer<? extends StreamDataEvent<T>> eventHandler);
    void onClose(Consumer<? extends StreamCloseEvent> eventHandler);
    void onError(Consumer<? extends StreamErrorEvent> eventHandler);
    void close(StreamCloseEvent event);
}
