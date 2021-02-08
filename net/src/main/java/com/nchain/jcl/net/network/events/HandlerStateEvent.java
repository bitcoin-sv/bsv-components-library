package com.nchain.jcl.net.network.events;


import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.tools.handlers.HandlerState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Handler publishes its current State.
 */
public final class HandlerStateEvent extends Event {
    private final HandlerState state;

    public HandlerStateEvent(HandlerState state)    { this.state = state; }
    public HandlerState getState()                  { return this.state; }
    @Override
    public String toString()                        { return "Event[State]: " + state.toString(); }

}
