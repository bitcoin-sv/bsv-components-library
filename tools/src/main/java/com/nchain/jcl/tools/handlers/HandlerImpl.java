package com.nchain.jcl.tools.handlers;


import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.events.EventBus;

import java.time.Duration;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A base implementation of a Handler that must be extended by any Handler
 * (NOTE: An exception to this is the NetworkHandler which already extends another guavba class, for it's not
 * possible for him to also extend this one). The implementation provides a Map used to store handler Peer information. For example,
 * a PingPongHandler uses the Map to store the state of each connected Peer. A Blocking Map is used as a message might come through to handler
 * before the PeerConnectedEvent, so it gives the handler time for the peer to be registered.
 */
public abstract class HandlerImpl<K, V> implements Handler {

    // for logging:
    private String id;

    // Runtime Configuration
    protected RuntimeConfig runtimeConfig;

    // Event Bus used to trigger events and register callbacks for them
    protected EventBus eventBus;


    protected SimpleBlockingMap<K, V> handlerInfo = new SimpleBlockingMap<>();

    private static final Duration DEFAULT_HANDSHAKE_TIMEOUT_MS = Duration.ofMillis(50);

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


    protected V getOrWaitForHandlerInfo(K peerAddress){
        try {
            V peerInfo = handlerInfo.take(peerAddress, DEFAULT_HANDSHAKE_TIMEOUT_MS);
            return peerInfo;
        } catch (InterruptedException ex){
            throw new RuntimeException(ex);
        }
    }

}
