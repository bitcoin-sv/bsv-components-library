package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.events.P2PEvent;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 23/08/2021
 *
 * An Event triggered when a series of messages have been received, all of them of the same type. They might be
 * received from multiple and different Peers.
 * The number of events within the Batch is configured by the app.
 */
public class MsgReceivedBatchEvent<T extends MsgReceivedEvent> extends P2PEvent  {
    private List<T> events;

    public MsgReceivedBatchEvent(List<T> events) {
        this.events = events;
    }

    public List<T> getEvents() { return this.events;}

    @Override
    public String toString() {
        return "MsgReceivedBatch[" + events.size() + " items]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false;}
        MsgReceivedBatchEvent other = (MsgReceivedBatchEvent) obj;
        return Objects.equal(this.events, other.events);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), events);
    }
}
