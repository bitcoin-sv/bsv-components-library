package io.bitcoinsv.jcl.net.network.events;

import com.google.common.base.Objects;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event Triggered when a connection to a list of Peers has been Rejected. So the connectin never took place in
 * the first place.
 * This class is an aggregation of PeerRejectedEvent. For performance reasons, sometimes its beter to trigger a
 * collection of Events instead of individual events for each Peer.
 */
public final class PeersRejectedEvent extends P2PEvent {

    private final List<PeerRejectedEvent> events;

    public PeersRejectedEvent(List<PeerRejectedEvent> events) {
        this.events = events;
    }

    public List<PeerRejectedEvent> getEvents() {
        return this.events;
    }

    @Override
    public String toString() {
        return "Event[PeersRejected]: " + events.size() + "peers rejected";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        PeersRejectedEvent other = (PeersRejectedEvent) obj;
        return Objects.equal(this.events, other.events);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), events);
    }
}
