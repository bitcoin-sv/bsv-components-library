package io.bitcoinsv.jcl.net.network.events;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.tools.handlers.HandlerState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Handler publishes its current State.
 */
public final class HandlerStateEvent extends P2PEvent {
    private final HandlerState state;

    public HandlerStateEvent(HandlerState state)    { this.state = state; }
    public HandlerState getState()                  { return this.state; }

    @Override
    public String toString()                        { return "Event[State]: " + state.toString(); }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        HandlerStateEvent other = (HandlerStateEvent) obj;
        return Objects.equal(this.state, other.state);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), state);
    }
}
