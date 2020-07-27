package com.nchain.jcl.protocol.handlers.pingPong;

import com.nchain.jcl.tools.handlers.HandlerState;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:48
 *
 * This event stores the state of the PingPong Handler at a point in time.
 * The PingPong Handler takes care of checking that the Remote Peers we are connected to are still
 * "alive". On a frequency basis, it sends a PING message to them, expecting a PONG message back. If
 * the response does not come or comes out of time, the Peer has then broken the timeout and will most
 * probably be blacklisted.
 */
@Value
@Builder(toBuilder = true)
public class PingPongHandlerState extends HandlerState {
    private long numPingInProcess;

    @Override
    public String toString() {
        return "PingPong-Handler State: " + numPingInProcess + " Pings in progress";
    }
}
