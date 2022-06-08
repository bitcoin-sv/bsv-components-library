package io.bitcoinsv.jcl.net.network.events;


import com.google.common.base.Objects;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Stop Connecting to more Peers in the Network.
 * This Request is usually triggered when we reach the minimum number of desired connections.
 */
public final class StopConnectingRequest extends P2PRequest {
    public StopConnectingRequest() {}

    @Override
    public String toString() {
        return "StopConnectingRequest()";
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
