package com.nchain.jcl.tools.handlers.common;

import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-18 17:43
 *
 * Base class for a Handler Status. A "Status" represents a view of the Handler at a
 * specific point in time.
 * Each specific handler can extend this class and create a custom workingState, including all the info
 * relevant (number of Peer connected, number of Errors, etc).
 *
 */
@Getter
public abstract class HandlerState {
    // The time the State has been taken.
    protected long timestamp;

    /** Constructor */
    public HandlerState() {
        this(System.currentTimeMillis());
    }
    /** Constructor */
    public HandlerState(long timestamp) {
        this.timestamp = timestamp;
    }

    public void updateTimestamp() { this.timestamp = System.currentTimeMillis();}
}
