package io.bitcoinsv.bsvcl.net.protocol.wrapper;


import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler;
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandlerImpl;
import io.bitcoinsv.bsvcl.net.protocol.handlers.handshake.HandshakeHandler;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.config.NetworkConfig;
import io.bitcoinsv.bsvcl.net.network.config.provided.NetworkDefaultConfig;
import io.bitcoinsv.bsvcl.net.network.events.HandlerStateEvent;
import io.bitcoinsv.bsvcl.net.network.handlers.NetworkHandler;
import io.bitcoinsv.bsvcl.net.network.handlers.NetworkHandlerImpl;
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault;
import io.bitcoinsv.bsvcl.common.events.EventBus;
import io.bitcoinsv.bsvcl.common.handlers.Handler;
import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;
import io.bitcoinsv.bsvcl.common.handlers.HandlerConfig;
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class is a wrapper over all the P2P and Network Handlers needed to connect to the P2P Network and
 * run the Bitcoin P2P. It provides convenience classes for subscribing to a Stream of Events, and for
 * send Request/Orders/Messages.
 */
public class P2P {
    // Id, for logging purposes mostly:
    private String id;

    private LoggerUtil logger;

    // Configurations:
    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;
    private ProtocolConfig protocolConfig;

    // Event Bus that will be used to "link" all the Handlers together
    private EventBus eventBus;

    // Specific EventBus to trigger Handler States. We use a dedicated Handler for triggering the Handlers States, so
    // we make sure that the states are always triggered even when the system is under a heavy load and all the
    // "regular" eventBus is too busy:
    private EventBus stateEventBus;

    // Map of all the Handlers included in this wrapper:
    private Map<String, Handler> handlers = new ConcurrentHashMap<>();

    // Map storing the different Frequencies at which the States of the different Handlers are published into
    // the Bus
    private Map<String, Duration> stateRefreshFrequencies = new HashMap<>();

    // A ExecutorService, to trigger the Thread to call the getStatus() on the Handlers:
    private ScheduledExecutorService executor;

    // Latch that is released when start has finished.
    private final CountDownLatch startedLatch = new CountDownLatch(1);

    // Event Stream Managers Definition:
    public final P2PEventStreamer EVENTS;

    // Request Handler:
    public final P2PRequestHandler REQUESTS;

    /** Constructor */
    public P2P(String id, RuntimeConfig runtimeConfig, NetworkConfig networkConfig, ProtocolConfig protocolConfig) {
        try {
            this.id = id;
            this.logger = new LoggerUtil(id, "P2P Service", this.getClass());
            // We update the Configurations
            this.runtimeConfig = runtimeConfig;
            this.networkConfig = networkConfig;
            this.protocolConfig = protocolConfig;

            // EventBus for the Internal Handles within the P2P Service:
            this.eventBus = EventBus.builder()
                    .build();

            // EventBus for Handlers State Publishing:
            this.stateEventBus = EventBus.builder()
                    .build();

            // Event Streamer:
            EVENTS = new P2PEventStreamer(this.eventBus, this.stateEventBus);

            // Requests Handlers:
            REQUESTS = new P2PRequestHandler(this.eventBus);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    /** Constructor with Default values */
    public P2P() {
        this("protocolHandler", new RuntimeConfigDefault(), new NetworkDefaultConfig(), new ProtocolBSVMainConfig());
    }

    protected void addHandler(Handler handler) {
        handler.useEventBus(this.eventBus);
        handler.init();
        this.handlers.put(handler.getId(), handler);
    }

    public Handler getHandler(String handlerId) {
        return this.handlers.get(handlerId);
    }

    protected void refreshHandlerState(String handlerId, Duration frequency) {
        this.stateRefreshFrequencies.put(handlerId, frequency);
    }

    private void init() {
        try {
            // If specified, we trigger a new Thread that will publish the status of the Handlers into the
            // Bus. The Map contains a duration for each Handler Class, so we can set up a different frequency for each
            // State notification...

            if (stateRefreshFrequencies != null && stateRefreshFrequencies.size() > 0) {
                this.executor = ThreadUtils.getSingleThreadScheduledExecutorService(id + "-HandlerStatusRefresh");
                for (Handler handler : handlers.values()) {
                    if (stateRefreshFrequencies.keySet().contains(handler.getId())) {
                        Duration frequency = stateRefreshFrequencies.get(handler.getId());
                        this.executor.scheduleAtFixedRate(() -> this.refreshStatusJob(handler), frequency.toMillis(),
                                frequency.toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            }

            // We log some useful info...

            logger.info("JCL-Net Configuration:");
            logger.info(" - " + protocolConfig.toString() + " configuration");
            logger.info(" - working dir: " + runtimeConfig.getFileUtils().getRootPath().toAbsolutePath());
            // We log the Peers range, if the Handshake Handler is enabled:
            if (handlers.containsKey(HandshakeHandler.HANDLER_ID)) {
                String minPeersStr = protocolConfig.getBasicConfig().getMinPeers().isEmpty() ? "(?" : "[" + protocolConfig.getBasicConfig().getMinPeers().getAsInt();
                String maxPeersStr = protocolConfig.getBasicConfig().getMaxPeers().isEmpty() ? "?)" :  protocolConfig.getBasicConfig().getMaxPeers().getAsInt() + "]";
                logger.info(" - peers range: " + minPeersStr + " - " + maxPeersStr);
            }
            logger.info("Thread Pool used: " + (runtimeConfig.useCachedThreadPoolForP2P()? "Cached" : "Fixed") + ", MaxThreads: " + runtimeConfig.getMaxNumThreadsForP2P());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void start() {
        logger.info("Starting...");
        NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to Start without it...");
        init();
        handler.start();
        startedLatch.countDown();
    }

    public void startServer() {
        logger.info("Starting (server mode)...");
        NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to Start without it...");
        init();
        handler.startServer();
        startedLatch.countDown();
    }

    /**
     * Wait for the P2P to be started.
     */
    public void awaitStarted() {
        try {
            startedLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait for the P2P to be started.
     */
    public boolean awaitStarted(long timeout, TimeUnit unit) {
        try {
            return startedLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /** Initiate a graceful shutdown of the P2P. */
    public void stop() throws InterruptedException {
        NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to Stop without it...");
        handler.stop();
        if (this.executor != null) this.executor.shutdownNow();
        logger.info("Stopping ...");
    }

    /**
     * Wait for the P2P to be stopped.
     */
    public void awaitStopped() {
        NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to Stop without it...");
        handler.awaitStopped();
    }

    // convenience method to return the PeerAddress for this ProtocolHandler. It assumes that there is a NetworkHandler
    public PeerAddress getPeerAddress() {
        try {
            // The localAddress used by NetworkHandlerImpl doesn't work, since most probably will be "0.0.0.0", which
            // means its listening in all network adapters. We need the local IP, which we obtain by using directly
            // the Local Inet4Address and we get the port from NetworkHandlerImpl...

            NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
            if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to getPeerAddress without it...");
// This way of getting local IP address might cause issues if the host has several network interfaces:
//            PeerAddress result = PeerAddress.fromIp(
//                    Inet4Address.getLocalHost().getHostAddress() + ":" + handler.getPeerAddress().getPort());
            PeerAddress result = PeerAddress.fromIp(("127.0.0.1") + ":" + handler.getPeerAddress().getPort());
            return result;
        } catch (UnknownHostException e) {
            logger.error("Error getting P2P Address");
            throw new RuntimeException(e);
        }
    }

    // convenience method to return the PeerAddress for this ProtocolHandler. It assumes that there is a BlacklistHandler
    public BlacklistHandler getBlacklistHandler() {
        BlacklistHandler handler = (BlacklistHandler) handlers.get(BlacklistHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Blacklist Handler Found.");
        return handler;
    }

    // Runs of an scheduled basis, and invoke the "getState()" method on all the Handlers, and then publishes the
    // states as Event into the Bus, so they can be "streamed" by the User.
    private void refreshStatusJob(Handler handler) {
        try {
            //System.out.println("----> Trying to Publish State for " + handler.getId() + "...");
            HandlerStateEvent event = new HandlerStateEvent(handler.getState());
            stateEventBus.publish(event);
            //System.out.println("----> State for " + handler.getId() + " Published.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateConfig(String handlerId, HandlerConfig config) {
        if (!handlers.containsKey(handlerId)) {
            throw new RuntimeException("Handler not present in the P2P Object");
        }
        handlers.get(handlerId).updateConfig(config);
        logger.info("Handler " + handlerId + " Config Updated.");
    }

    // Convenience method to deserialize a reference to a P2PBuilder
    public static P2PBuilder builder(String id) {
        return new P2PBuilder(id);
    }

    public RuntimeConfig getRuntimeConfig()     { return this.runtimeConfig; }
    public NetworkConfig getNetworkConfig()     { return this.networkConfig; }
    public ProtocolConfig getProtocolConfig()   { return this.protocolConfig; }
    public EventBus getEventBus()               { return this.eventBus;}
}