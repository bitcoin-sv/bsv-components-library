package com.nchain.jcl.tools.handlers.listeners;


import com.nchain.jcl.tools.concurrent.ListenerExecutor;
import com.nchain.jcl.tools.concurrent.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-29
 *
 * An Utility class to wrap the execution of multiple Listeners
 */
public class HandlerListenerExecutor extends ListenerExecutor {

    // List of Listeners:
    private List<HandlerStartListener> startListeners = new ArrayList<>();
    private List<HandlerStopListener> stopListeners = new ArrayList<>();

    // Constructor
    public HandlerListenerExecutor(ExecutorService executor) {
        super(executor);
    }

    // Methods to add listeners:
    public void addListener(HandlerStartListener callback)          { startListeners.add(callback);}
    public void addListener(HandlerStopListener callback)           { stopListeners.add(callback);}


    // The following methods execute one specific list of Listeners:

    public void executeStartListeners() {
        Runnable task = () -> startListeners.forEach(c -> wrap(() -> c.onStart()));
        runTask(task);
    }

    public void executeStopListeners() {
        Runnable task = () -> stopListeners.forEach(c -> wrap(() -> c.onStop()));
        runTask(task);
    }

}
