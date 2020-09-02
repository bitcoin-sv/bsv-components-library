package com.nchain.jcl.net.protocol.handlers.handshake;

import com.nchain.jcl.base.tools.handlers.HandlerState;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-06 13:00
 *
 * This event stores the state of the Handshake Handler at a point in time.
 * the Handshake Handler takes care of implementing the Handshake Protocol, which takes place
 * right after connecting to a new Peer. It consists of a exchange of messages between the 2
 * parties to verify that they are protocol-compatible.
 *
 * This event stores the number of Peers currently handshaked or failed, and also some flags
 * that indicate if the Service has been requested to look for more Peers or to stop new connections
 * instead (these request to resume/stop connections are always triggered when the nuymber of
 * Peer handshakes go above or below some thresholds).
 */
@Value
@Builder(toBuilder = true)
public class HandshakeHandlerState extends HandlerState {
    @Builder.Default
    private int numCurrentHandshakes = 0;
    @Builder.Default
    private BigInteger numHandshakesFailed = BigInteger.ZERO;
    @Builder.Default
    private boolean moreConnsRequested = true;
    private boolean stopConnsRequested;

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();

        result.append("Handshake Handler State: ");
        result.append(numCurrentHandshakes + " current Handshakes, ");
        result.append(numHandshakesFailed + " failed. ");
        if (moreConnsRequested) result.append(" More Connections requested");
        if (stopConnsRequested) result.append(" Stop Connections requested");

        return result.toString();
    }
}
