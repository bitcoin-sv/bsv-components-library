package com.nchain.jcl.tools.streams;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-03 10:37
 *
 * An InputStream is an abstraction that works as a source of Data, data being sent by another party of which
 * we do not have control. Since we cannot control when the data is sent, we can only assign callbacks/EventHandlers
 * that will get triggered when the data arrives.
 *
 * We can get notified of 2 types of Events:
 *  - When some data arrives
 *  - When the data stream is closed.
 *
 *  In both cases we can define callbacks that will be triggered on those scenarios. Each scenario is represented by
 *  an specific event, and the callback is just a Consumer of that Event.
 *  We can add more than one callback/eventHandler to one event, or nothing at all.
 *
 *  An InputStream does NOT provide information about the SOURCE of the data. That information can be specified
 *  when creating instances that implement this class
 *
 */
public interface InputStream<T> {
    void onData(Consumer<? extends StreamDataEvent<T>> eventHandler);
    void onClose(Consumer<? extends StreamCloseEvent> eventHandler);
}
