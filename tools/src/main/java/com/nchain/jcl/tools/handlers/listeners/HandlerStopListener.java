package com.nchain.jcl.tools.handlers.listeners;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-19 14:13
 *
 * A Listener triggered when a Protocol Handler stops.
 */
public interface HandlerStopListener {

    /** Triggered when the Handler has stopped */
    void onStop();
}
