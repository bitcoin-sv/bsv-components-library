package io.bitcoinsv.bsvcl.tools.handlers;


import io.bitcoinsv.bsvcl.tools.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.tools.events.Event;
import io.bitcoinsv.bsvcl.tools.events.EventBus;

import java.time.Duration;
import java.util.function.Consumer;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A base implementation of a Handler that must be extended by any Handler.
 * (NOTE: An exception to this is the NetworkHandler which already extends another guava class, for it's not
 * possible for him to also extend this one).
 * Since each Handler works with Peers, this implementatio includes a Map to store PeerInfo. Its very common that
 * for each Msg that a Handler performs, the first thing to do is to check if the Peer is already registered
 * in this map, and discard the Msg if its now (the Peer is registered in the Map when we receive the OnPeerHandshakedEvent)
 *
 * Sometimes, and due to the inherent reandomness orders of the Events triggered in a Multithread env, the Msgs from a
 * Handshaked Peer might arrive BEFORe the onPeerHandshakedEvent itself, so in that case the MSg will arrive when the
 * Peer is not registered yet. To deal with this issue, the Map in this implementation is a Blocking Map, which gets
 * the Info linked to a given key but it can also WAIT a bit if the Value is not there.
 *
 * So in case we get a Msg from a Peer but that Peer is not handhaked yet, the Blocking Map will wait a bit, enought for
 * the onPeerHandshakedEvent to arrive and regiser the Peer in the map.
 *
 * @param <K> Key used in the Map to identity a Peer
 * @param <V> Value/class used to store info about the Peer. Different for each Handler.
 *
 */
public abstract class HandlerImpl<K, V> implements Handler {

    // for logging:
    private String id;

    // Runtime Configuration
    protected RuntimeConfig runtimeConfig;

    // Event Bus used to trigger events and register callbacks for them
    protected EventBus eventBus;

    // A BlockingMap that stores info  fore each Peer
    protected SimpleBlockingMap<K, V> handlerInfo = new SimpleBlockingMap<>();

    // Timeout we are willing to wait if we try t get info from a Peer and that info is still not registered in the
    // Map This timeout should be enough for the PeerHandshakedEvent to arrive, in case there is such race condition
    private static final Duration DEFAULT_HANDSHAKE_TIMEOUT_MS = Duration.ofMillis(50);

    protected <E extends Event> void subscribe(Class<E> eventClass, Consumer<E> eventHandler) {
        eventBus.subscribe((Class<Event>) eventClass, (Consumer<Event>) eventHandler);
    }

    @Override
    public void useEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    // Constructor
    public HandlerImpl(String id, RuntimeConfig runtimeConfig) {
        this.id = id;
        this.runtimeConfig = runtimeConfig;
    }

    protected V getOrWaitForHandlerInfo(K peerAddress){
        try {
            V peerInfo = handlerInfo.take(peerAddress, DEFAULT_HANDSHAKE_TIMEOUT_MS);
            return peerInfo;
        } catch (InterruptedException ex){
            throw new RuntimeException(ex);
        }
    }
}