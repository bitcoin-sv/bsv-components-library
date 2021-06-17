package com.nchain.jcl.net.protocol.wrapper;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.config.NetworkConfig;
import com.nchain.jcl.net.network.config.provided.NetworkDefaultConfig;
import com.nchain.jcl.net.network.events.HandlerStateEvent;
import com.nchain.jcl.net.network.handlers.NetworkHandler;
import com.nchain.jcl.net.network.handlers.NetworkHandlerImpl;
import com.nchain.jcl.net.protocol.config.ProtocolConfig;
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig;
import com.nchain.jcl.net.protocol.events.data.MsgReceivedEvent;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandler;
import com.nchain.jcl.net.protocol.messages.FeeFilterMsg;
import com.nchain.jcl.net.protocol.messages.InvMessage;
import com.nchain.jcl.net.protocol.messages.PingMsg;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault;
import com.nchain.jcl.tools.events.Event;
import com.nchain.jcl.tools.events.EventBus;
import com.nchain.jcl.tools.handlers.Handler;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.thread.ThreadUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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

    // Map of all the Handlers included in this wrapper:
    private Map<String, Handler> handlers = new ConcurrentHashMap<>();

    // Map storing the different Frequencies at which the States of the different Handlers are published into
    // the Bus
    private Map<String, Duration> stateRefreshFrequencies = new HashMap<>();

    // A ExecutorService, to trigger the Thread to call the getStatus() on the Handlers:
    private ScheduledExecutorService executor;

    // Event Stream Managers Definition:
    public final P2PEventStreamer EVENTS;

    // Request Handler:
    public final P2PRequestHandler REQUESTS;

    // Low priority messages:
    // We use this list to define a Function that will be used in the EventBus to set the priority for each Message.
    // By default, all Event are HIGH-Priority messages, and the Low-Priority ones are those specified here:
    Set<Class> lowPriorityMessages = new HashSet<>( Arrays.asList(
            MsgReceivedEvent.class));

    /** Constructor */
    public P2P(String id, RuntimeConfig runtimeConfig, NetworkConfig networkConfig, ProtocolConfig protocolConfig) {
        try {
            this.id = id;
            this.logger = new LoggerUtil(id, "P2P-Handler", this.getClass());
            // We update the Configurations
            this.runtimeConfig = runtimeConfig;
            this.networkConfig = networkConfig;
            this.protocolConfig = protocolConfig;

            // We initialize the EventBus...
            // First we define the function that will be used to determine the Priority of each Event published to the
            // Bus:
            Function<Event, EventBus.ConsumerPriority> eventPriorityChecker = (Event e) -> {
                return (lowPriorityMessages.contains(e.getClass()))
                        ? EventBus.ConsumerPriority.NORMAL
                        : EventBus.ConsumerPriority.HIGH;
            };

            // Now we crate the EventBus, specifying 2 executor for LOW and High priority Events:
            this.eventBus = EventBus.builder()
                   .executor(ThreadUtils.EVENT_BUS_EXECUTOR)
                   .executorHighPriority(ThreadUtils.EVENT_BUS_EXECUTOR_HIGH_PRIORITY)
                   .eventPriorityChecker(eventPriorityChecker)
                   .build();


            // Event Streamer:
            EVENTS = new P2PEventStreamer(this.eventBus);

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
    }

    public void startServer() {
        logger.info("Starting (server mode)...");
        NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to Start without it...");
        init();
        handler.startServer();
    }

    public void stop() {
        NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to Stop without it...");
        handler.stop();
        if (this.executor != null) this.executor.shutdownNow();
        logger.info("Stop.");
    }

    // convenience method to return the PeerAddress for this ProtocolHandler. It assumes that there is a NetworkHandler
    public PeerAddress getPeerAddress() {
        NetworkHandler handler = (NetworkHandler) handlers.get(NetworkHandlerImpl.HANDLER_ID);
        if (handler == null) throw new RuntimeException("No Network Handler Found. Impossible to getPeerAddress without it...");
        return handler.getPeerAddress();
    }

    // Runs of an scheduled basis, and invoke the "getState()" method on all the Handlers, and then publishes the
    // states as Event into the Bus, so they can be "streamed" by the User.
    private void refreshStatusJob(Handler handler) {
            HandlerStateEvent event = new HandlerStateEvent(handler.getState());
            eventBus.publish(event);
            //System.out.println("Tasks in progress: " + eventBus.getTraceNumTasks());
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
