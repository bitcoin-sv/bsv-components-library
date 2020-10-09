package com.nchain.jcl.base.tools.handlers;

import com.nchain.jcl.base.tools.events.EventBus;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Handler is a Class that takes care of an specific tasks, and also works in collaboration with other Handlers.
 * The communication with the other handlers is done by sending/receiving messages through an EventBus, which is
 * an IN-Memory Publish/Subscribe implementation
 *
 * Example of a Handler is the HandshakeHandler, or the PingPongHandler, both take care of specific tasks in the
 * bitcoin protocol.
 */
public interface Handler {
    /** Returns the Handler Id */
    String getId();
    /** Returns the Handler Config */
    HandlerConfig getConfig();
    /** Returns the current State of this Handler */
    HandlerState getState();
    /** Links to this Handler to this EventBus, for events subscription and publishing */
    void useEventBus(EventBus eventBus);
    /** Initializes the Handler. It must be called right after "useEventBus()" */
    void init();
}
