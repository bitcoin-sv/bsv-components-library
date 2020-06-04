package com.nchain.jcl.tools.handlers.common;

import com.nchain.jcl.tools.handlers.listeners.HandlerStartListener;
import com.nchain.jcl.tools.handlers.listeners.HandlerStopListener;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-19 18:13
 *
 * This interface represents the basic that any Handler (a Network Handler, Protocl Handler or
 * any other) must provide.
 */
public interface Handler {

    /** Indicates f the Handler is running or not. */
    boolean isRunning();

    /**
     * Returns te Executor Service used by thi Handler to run the Listener.
     * If The listener a re running in bocking Mode, the Executor is not used.
     */
    ExecutorService getExecutor();
    /**
     * Protocol Handlers can be linked together, but all the relationships end up in a
     * Network Handler, which is the point of contact with the Network Package. This method
     * returns the Network Handler Id this Handler is ultimately connected to (for logging
     * purposes, mostly)
     */
    String getId();

    /** Returns the name of this Handler (for logging purposes, mostly) */
    String getName();

    /** Returns the Current State of this ProtocolHandler */
    HandlerState getState();

    /** Hook to be notified when the Handler starts */
    void addCallback(HandlerStartListener callback);

    /** Hook to be notified when the Handler stops */
    void addCallback(HandlerStopListener callback);

}
