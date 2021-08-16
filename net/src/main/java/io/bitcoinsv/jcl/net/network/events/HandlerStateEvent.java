/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.network.events;


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

}
