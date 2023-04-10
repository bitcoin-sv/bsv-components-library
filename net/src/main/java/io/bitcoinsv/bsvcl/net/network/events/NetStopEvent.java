package io.bitcoinsv.bsvcl.net.network.events;


import java.util.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when the Network Activity Stops
 */
public class NetStopEvent extends P2PEvent {
    @Override
    public String toString() {
        return "NetStopEvent()";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
