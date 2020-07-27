package com.nchain.jcl.network.handlers;

import com.nchain.jcl.tools.handlers.HandlerState;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 15:47
 *
 * This Event stores the current State of the Connection/Network Handler. The Network Handler implements the
 * physical and low-level connection to a remote Peers, and handles all the incoming/outcoming data between
 * the 2 parties.
 */
@Value
@Builder(toBuilder = true)
public class NetworkHandlerState extends HandlerState {
    private int numActiveConns;
    private int numInProgressConns;
    private int numPendingToOpenConns;
    private int numPendingToCloseConns;
    private boolean server_mode;
    private boolean keep_connecting;

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("Network Handler State: ");
        result.append("Connections: ");
        result.append(numActiveConns).append(" active, ");
        result.append(numInProgressConns).append(" in progress, ");
        result.append(numPendingToOpenConns).append( " pending to Open, ");
        result.append(numPendingToCloseConns).append(" pending to Close");
        result.append(": ").append((server_mode)? "Running in Server Mode" : "Running in Client Mode");
        result.append(": ").append((keep_connecting)? "connecting": "connections stable");
        return result.toString();
    }
}
