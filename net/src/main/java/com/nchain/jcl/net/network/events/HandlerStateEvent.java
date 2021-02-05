package com.nchain.jcl.net.network.events;


import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.tools.handlers.HandlerState;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Handler publishes its current State.
 */
@Value
@AllArgsConstructor
public class HandlerStateEvent extends Event {
    private HandlerState state;

    @Override
    public String toString() {
        return "Event[State]: " + state.toString();
    }
}
