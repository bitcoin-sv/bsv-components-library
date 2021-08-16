package io.bitcoinsv.jcl.net.network.events;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Stop Connecting to more Peers in the Network.
 * This Request is usually triggered when we reach the minimum number of desired connections.
 */
public final class StopConnectingRequest extends P2PRequest {
    public StopConnectingRequest() {}
    public String toString() {
        return "StopConnectingRequest()";
    }
}
