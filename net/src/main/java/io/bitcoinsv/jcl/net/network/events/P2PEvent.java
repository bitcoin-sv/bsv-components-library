package io.bitcoinsv.jcl.net.network.events;

import io.bitcoinsv.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-03-29
 *
 * Base class for any P2PEvent. distinction between P2PEvents and P2PRequests is: a P2P Event is something that
 * HAPPENED, while a P2PRequest is something you WANT to happen.
 */
public class P2PEvent extends Event {

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (this.getClass() != obj.getClass()) { return false;}
        return true;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
