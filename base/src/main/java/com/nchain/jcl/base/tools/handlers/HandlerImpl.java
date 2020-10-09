package com.nchain.jcl.base.tools.handlers;

import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.base.tools.events.EventBus;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A base implementation of a Handler that must be extended by any Handler
 * (NOTE: An exception to this is the NetworkHandler which already extends another guavba class, for it's not
 * possible for him to also extend this one)
 */
public abstract class HandlerImpl implements Handler {

    // for logging:
    private String id;

    // Runtime Configuration
    protected RuntimeConfig runtimeConfig;

    // Event Bus used to trigger events and register callbacks for them
    protected EventBus eventBus;

    @Override
    public void useEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    // Constructor
    public HandlerImpl(String id, RuntimeConfig runtimeConfig) {
        this.id = id;
        this.runtimeConfig = runtimeConfig;
    }

    // Initialization stuff
    public abstract void init();
}
