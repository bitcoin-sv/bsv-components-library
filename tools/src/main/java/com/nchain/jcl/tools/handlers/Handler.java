package com.nchain.jcl.tools.handlers;

import com.nchain.jcl.tools.events.EventBus;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 13:14
 *
 * A Handler is a Class that takes care of an specific tasks, and also works in collaboration with other Handlers.
 * The communication with the other handlers is done by sending/receiving messages through an EventBus, which is
 * an IN-Memory Publish/Subscribe implementation
 *
 * Example of a Handler is the HandshakeHandler, or the PingPongHandler, both take care of specific tasks in the
 * bitcoin protocol.
 */
public interface Handler {
    HandlerState getState();
    void useEventBus(EventBus eventBus);
}
