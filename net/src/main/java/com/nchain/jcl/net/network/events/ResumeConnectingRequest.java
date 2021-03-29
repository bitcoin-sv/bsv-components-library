package com.nchain.jcl.net.network.events;


import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event that represents a Request to Resume connecting to other Peers in the Network.
 * This Event is usually triggered when the number of connections has dropped below some
 * defined threshold.
 */
public final class ResumeConnectingRequest extends P2PRequest {
    public ResumeConnectingRequest() {}
    public String toString() {
        return "ResumeConnectingRequest()";
    }
}
