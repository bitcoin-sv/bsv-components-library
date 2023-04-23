package io.bitcoinsv.bsvcl.net;


import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler;
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandlerImpl;
import io.bitcoinsv.bsvcl.net.protocol.handlers.handshake.HandshakeHandler;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.events.HandlerStateEvent;
import io.bitcoinsv.bsvcl.net.network.NetworkController;
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault;
import io.bitcoinsv.bsvcl.common.events.EventBus;
import io.bitcoinsv.bsvcl.common.handlers.Handler;
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PEventStreamer;
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PRequestHandler;
import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;
import io.bitcoinsv.bsvcl.common.handlers.HandlerConfig;
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * The P2P class is the controller for accessing the Bitcoin P2P network.
 * <p>
 * Once started, a P2P object will start connecting to the Bitcoin P2P network in accordance with the
 * configuration with which it was created.
 * <p>
 * Events from the network will be streamed to the relevant Event Buses
 * <p>
 * Only one P2P object is expected to be instantiated.
 * <p>
 * BELOW IS THE PLAN, NOT IMPLEMENTED YET
 * The net packages use Java NIO to create connections. Java NIO uses asynchronous patterns to manage network
 * connections, with the Selector managing the connections. Our pattern is to instantiate as many threads as there
 * are cores in the system with a Selector for each thread. Connections are distributed across the Selectors. Each
 * Selector (with thread) is managed in the NetworkHandlerImpl.
 *
 * @author i.fernandez@nchain.com
 */
public class P2P {
    // id, for logging purposes mostly:
    private final String id;

    private final LoggerUtil logger;

    // Configurations:
    private final RuntimeConfig runtimeConfig;
    private final P2PConfig networkConfig;
    private final ProtocolConfig protocolConfig;

    // Event Bus that will be used to "link" all the Handlers together
    private final EventBus eventBus;

    // Specific EventBus to trigger Handler States. We use a dedicated Handler for triggering the Handlers States, so
    // we make sure that the states are always triggered even when the system is under a heavy load and all the
    // "regular" eventBus is too busy:
    private final EventBus stateEventBus;

    // The network controllers. At the moment we only have one.
    private final NetworkController networkController;

    // Map of all the message handlers
    private final Map<String, Handler> handlers = new ConcurrentHashMap<>();

    // Map storing the different Frequencies at which the States of the different Handlers are published into
    // the Bus
    private final Map<String, Duration> stateRefreshFrequencies = new HashMap<>();

    // A ExecutorService, to trigger the Thread to call the getStatus() on the Handlers:
    private ScheduledExecutorService executor;

    // Latch that is released when start has finished.
    private final CountDownLatch startedLatch = new CountDownLatch(1);

    // Event Stream Managers Definition:
    public final P2PEventStreamer EVENTS;

    // Request Handler:
    public final P2PRequestHandler REQUESTS;

    public P2P(String id, RuntimeConfig runtimeConfig, P2PConfig networkConfig, ProtocolConfig protocolConfig) {
        try {
            this.id = id;
            this.logger = new LoggerUtil(id, "P2P Controller", this.getClass());
            this.runtimeConfig = runtimeConfig;
            this.networkConfig = networkConfig;
            this.protocolConfig = protocolConfig;

            // set up the network controllers, at the moment we only have one
            String serverIp = "0.0.0.0:" + networkConfig.getListeningPort();
            this.networkController = new NetworkController(id, runtimeConfig, networkConfig, PeerAddress.fromIp(serverIp));

            // EventBus for the Internal Handles within the P2P Service:
            this.eventBus = EventBus.builder().build();

            // EventBus for Handlers State Publishing:
            this.stateEventBus = EventBus.builder().build();

            // Event Streamer:
            EVENTS = new P2PEventStreamer(this.eventBus, this.stateEventBus);

            // Requests Handlers:
            REQUESTS = new P2PRequestHandler(this.eventBus);

            this.networkController.useEventBus(this.eventBus);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public P2P() {
        this("p2pController", new RuntimeConfigDefault(), P2PConfig.builder().build(), new ProtocolBSVMainConfig());
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
        init();
        this.networkController.start();
        startedLatch.countDown();
    }

    public void startServer() {
        logger.info("Starting (server mode)...");
        init();
        this.networkController.startServer();
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
        this.networkController.stop();
        if (this.executor != null) this.executor.shutdownNow();
        logger.info("Stopping ...");
    }

    /**
     * Wait for the P2P to be stopped.
     */
    public void awaitStopped() {
        this.networkController.awaitStopped();
    }

    // convenience method to return the PeerAddress for this ProtocolHandler. It assumes that there is a NetworkHandler
    public PeerAddress getPeerAddress() {
        try {
            // The localAddress used by NetworkHandlerImpl doesn't work, since most probably will be "0.0.0.0", which
            // means its listening in all network adapters. We need the local IP, which we obtain by using directly
            // the Local Inet4Address and we get the port from NetworkHandlerImpl...

// This way of getting local IP address might cause issues if the host has several network interfaces:
//            PeerAddress result = PeerAddress.fromIp(
//                    Inet4Address.getLocalHost().getHostAddress() + ":" + handler.getPeerAddress().getPort());
            PeerAddress result = PeerAddress.fromIp(("127.0.0.1") + ":" + this.networkController.getPeerAddress().getPort());
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

    // Convenience method to get a reference to a P2PBuilder
    public static P2PBuilder builder(String id) {
        return new P2PBuilder(id);
    }

    public RuntimeConfig getRuntimeConfig()     { return this.runtimeConfig; }
    public P2PConfig getNetworkConfig()     { return this.networkConfig; }
    public ProtocolConfig getProtocolConfig()   { return this.protocolConfig; }
    public EventBus getEventBus()               { return this.eventBus;}
}