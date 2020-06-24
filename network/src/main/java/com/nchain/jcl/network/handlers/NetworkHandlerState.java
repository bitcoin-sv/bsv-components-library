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
 * This clsss stores the current State of a Connection Handler
 */
@Value
@Builder(toBuilder = true)
public class NetworkHandlerState extends HandlerState {
    private int numActiveConns;
    private int numPendingToOpenConns;
    private int numPendingToCloseConns;
    private boolean server_mode;
    private boolean keep_connecting;
}
