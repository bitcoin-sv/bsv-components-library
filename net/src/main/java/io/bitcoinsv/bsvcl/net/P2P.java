package io.bitcoinsv.bsvcl.net;


import com.google.common.util.concurrent.Service;
import io.bitcoinsv.bsvcl.common.thread.TimeoutTask;
import io.bitcoinsv.bsvcl.common.thread.TimeoutTaskBuilder;
import io.bitcoinsv.bsvcl.net.network.NetworkListener;
import io.bitcoinsv.bsvcl.net.network.events.*;
import io.bitcoinsv.bsvcl.net.network.streams.nio.NIOStream;
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler;
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandlerImpl;
import io.bitcoinsv.bsvcl.net.protocol.handlers.handshake.HandshakeHandler;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The P2P class is the controller for accessing the Bitcoin P2P network.
 * <p>
 * Once started, a P2P object will start connecting to the Bitcoin P2P network in accordance with the
 * configuration with which it was created.
 * <p>
 * Events from the network will be streamed to the relevant Event Buses
 * <p>
 * Only one P2P object is expected to be instantiated but having more is useful for testing purposes.
 * <p>
 * BELOW IS THE PLAN, NOT IMPLEMENTED YET
 * The net packages use Java NIO to create connections. Java NIO uses asynchronous patterns to manage network
 * connections, with the Selector managing the connections. Our pattern is to instantiate as many threads as there
 * are cores in the system with a Selector for each thread. Connections are distributed across the Selectors. Each
 * Selector (with thread) is managed in the NetworkController.
 * <p>
 * Since the P2P object is responsible for distributing the connections across the Selectors, it needs its own thread.
 *
 * @author i.fernandez@nchain.com
 */
public class P2P extends Thread {
    // id, for logging purposes mostly:
    private final String id;

    private final LoggerUtil logger;

    // Configurations:
    private final RuntimeConfig runtimeConfig;
    private P2PConfig networkConfig;
    private final ProtocolConfig protocolConfig;

    // Event Bus that will be used to "link" all the Handlers together
    private final EventBus eventBus;

    // Specific EventBus to trigger Handler States. We use a dedicated Handler for triggering the Handlers States, so
    // we make sure that the states are always triggered even when the system is under a heavy load and all the
    // "regular" eventBus is too busy:
    private final EventBus stateEventBus;

    // The network controllers. At the moment we only have one.
    private NetworkController networkController = null;
    // the network listener
    private final NetworkListener networkListener;
    // Map of all the message handlers
    private final Map<String, Handler> handlers = new ConcurrentHashMap<>();

    // Map storing the different Frequencies at which the States of the different Handlers are published into
    // the Bus
    private final Map<String, Duration> stateRefreshFrequencies = new HashMap<>();

    // A ExecutorService, to trigger the Thread to call the getStatus() on the Handlers:
    private ScheduledExecutorService executor;

    // Latch that is released when start has finished.
    private final CountDownLatch startedLatch = new CountDownLatch(1);
    // Latch to indicate that the P2P should shut down
    private final CountDownLatch stopLatch = new CountDownLatch(1);
    // Semaphore that is incremented when there are events to process.
    private final Semaphore eventsToProcess = new Semaphore(1);
    // The active connections. Note that this class considers a connection active as soon as a NetworkController
    // has been instructed to open it.
    private final Map<PeerAddress, NetworkController> activeConns = new ConcurrentHashMap<>();
    // the connections that are waiting to be opened
    private final BlockingQueue<PeerAddress> pendingToOpenConns = new LinkedBlockingQueue<>();
    // the connections that are waiting to be closed
    private final BlockingQueue<DisconnectPeerRequest> pendingToCloseConns = new LinkedBlockingQueue<>();
    // the addresses which have been tried and are now closed
    private final Set<PeerAddress> closedConns = ConcurrentHashMap.newKeySet();
    // the addresses which are blacklisted
    private final Set<InetAddress> blacklist = ConcurrentHashMap.newKeySet();
    // the addresses which have failed to connect
    private final Set<PeerAddress> failedConns = ConcurrentHashMap.newKeySet();

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
            String serverIp = "0.0.0.0:" + networkConfig.getListeningPort();
            this.networkListener = new NetworkListener(id, this, PeerAddress.fromIp(serverIp));
            // EventBus for the internal handlers within the P2P sub-system
            this.eventBus = EventBus.builder().build();

            // EventBus for Handlers State Publishing:
            this.stateEventBus = EventBus.builder().build();

            // Event Streamer:
            EVENTS = new P2PEventStreamer(this.eventBus, this.stateEventBus);

            // Requests Handlers:
            REQUESTS = new P2PRequestHandler(this.eventBus);

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

    /** Add a Peer to the list of Peers to connect to. */
    public void connect(PeerAddress peerAddress) {
        connect(Collections.singletonList(peerAddress));
    }

    /** Add a list of Peers to the list of Peers to connect to. */
    public void connect(List<PeerAddress> peerAddresses) {
        if (peerAddresses == null) return;
        // remove the Peers we are already connected to, or in process to...
        List<PeerAddress> listToAdd = peerAddresses.stream()
                .filter(p -> !activeConns.containsKey(p))
                .filter(p -> !pendingToOpenConns.contains(p))
                .filter(p -> !pendingToCloseConns.contains( new PeerAddress2DisconnectPeerRequest_Comparator(p) ))
                .filter(p -> !blacklist.contains(p.getIp()))
                .collect(Collectors.toList());

        if (listToAdd.size() > 0) {
            // Check that the limit in the Pending Socket Connections is not exceeded
            // If there is no limit, we just include them all. If there is a limit, we only include them up
            // to the limit.
            List<PeerAddress> finalListToAdd = listToAdd;

            int limit = networkConfig.getMaxSocketPendingConnections();
            int numItemsToAdd = Math.min(finalListToAdd.size(), limit - pendingToOpenConns.size());
            if (numItemsToAdd > 0)
                finalListToAdd = listToAdd.subList(0, numItemsToAdd);
            else finalListToAdd = new ArrayList<>(); // empty List
            pendingToOpenConns.addAll(finalListToAdd);
            eventsToProcess.release();
        }
    }

    public void disconnect(PeerAddress peerAddress) {
        processDisconnectRequest(new DisconnectPeerRequest(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL));
    }

    public void disconnect(List<PeerAddress> peerAddressList) {
        processDisconnectRequests(peerAddressList
                .stream()
                .map(p -> new DisconnectPeerRequest(p, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL))
                .collect(Collectors.toList()));
    }

    public void blacklist(InetAddress ipAddress, PeersBlacklistedEvent.BlacklistReason reason) {
        Map<InetAddress, PeersBlacklistedEvent.BlacklistReason> map = new HashMap<>();
        map.put(ipAddress, reason);
        blacklist(map);
    }

    public void blacklist(Map<InetAddress, PeersBlacklistedEvent.BlacklistReason> ipAddresses) {
        if (ipAddresses == null) return;
        // First, we add the IpAddress to the Blacklist, to keep a reference to them.
        blacklist.addAll(ipAddresses.keySet());

        // Then, we disconnect all the current Peers already connected to any of those addresses...
        List<DisconnectPeerRequest> requestsToDisconnect = this.activeConns.keySet().stream()
                .filter(p -> ipAddresses.keySet().contains(p.getIp()))
                .map(p -> new DisconnectPeerRequest(p, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL_BLACKLIST))
                .collect(Collectors.toList());
        this.processDisconnectRequests(requestsToDisconnect);
    }

    public void removeFromBlacklist(List<InetAddress> ipAddresses) {
        if (ipAddresses == null) return;
        ipAddresses.forEach(blacklist::remove);
    }

    /** Called by the listener when a new connection is accepted. */
    public void acceptConnection(PeerAddress peerAddress, SocketChannel channel) {
        // this has concurrency issues that I will deal with soon
        // check for duplicate connection
        if (activeConns.containsKey(peerAddress)) {
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("Error closing duplicate connection from {}", peerAddress, e);
            }
            return;
        }
        // check the blacklist
        if (blacklist.contains(peerAddress.getIp())) {
            logger.trace("{} : {} : discarding incoming connection (blacklisted).", this.id, peerAddress.getIp());
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("Error closing blacklisted connection from {}", peerAddress, e);
            }
            return;
        }
        // check the max connections
        if (activeConns.size() >= networkConfig.getMaxSocketConnections()) {
            logger.trace("{} : {} : no more connections allowed ({})", this.id, peerAddress, networkConfig.getMaxSocketConnections());
            try {
                channel.close();
            } catch (IOException e) {
                logger.error("Error closing connection when exceeded max from {}", peerAddress, e);
            }
            return;
        }
        // send the connection to a NetworkController
        // at the moment we only have one NetworkController
        networkController.acceptConnection(peerAddress, channel);
        activeConns.put(peerAddress, networkController);
    }

    public void run() {
        try {
            // register for events first, so we don't miss any
            registerForEvents();

            // set up the network controllers, at the moment we only have one
            String serverIp = "0.0.0.0:" + networkConfig.getListeningPort();
            this.networkController = new NetworkController(id, runtimeConfig, networkConfig, PeerAddress.fromIp(serverIp));
            this.networkController.useEventBus(this.eventBus);
            this.networkController.start();

            if (networkConfig.isListening()) {
                this.networkListener.start();
            }

            // If specified, we trigger a new Thread that will publish the status of the Handlers into the
            // Bus. The Map contains a duration for each Handler Class, so we can set up a different frequency for each
            // State notification...
            if (stateRefreshFrequencies.size() > 0) {
                this.executor = ThreadUtils.getSingleThreadScheduledExecutorService(id + "-HandlerStatusRefresh");
                for (Handler handler : handlers.values()) {
                    if (stateRefreshFrequencies.containsKey(handler.getId())) {
                        Duration frequency = stateRefreshFrequencies.get(handler.getId());
                        this.executor.scheduleAtFixedRate(() -> this.refreshStatusJob(handler), frequency.toMillis(),
                                frequency.toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            }

            // log some useful info...
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

            startedLatch.countDown();   // signal that the P2P is started

            // main processing, to be extended
            do {
                eventsToProcess.acquire();
                handlePendingToCloseConnections();
                handlePendingToOpenConnections();
            } while (stopLatch.getCount() != 0);

            // shutdown
            logger.info("Stopping ...");
            networkController.initiateStop();
            if (this.executor != null) this.executor.shutdownNow();
        } catch (InterruptedException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public void startServer() {
        logger.info("Starting (server mode)...");
        networkConfig = networkConfig.toBuilder().listening(true).build();
        start();
    }

    /**
     * Wait for the P2P to be started.
     */
    public void awaitStarted() {
        try {
            startedLatch.await();
            networkController.awaitStarted();
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
    public void initiateStop() {
        stopLatch.countDown();
        eventsToProcess.release();
    }

    /**
     * Wait for the P2P to be stopped.
     */
    public void awaitStopped() {
        this.networkController.awaitStopped();
    }

    // convenience method to return the PeerAddress for this ProtocolHandler. It assumes that there is a NetworkHandler
    // todo: rename this
    public PeerAddress getPeerAddress() throws InterruptedException {
        try {
            if (networkConfig.isListening()) {
                networkListener.awaitInitialization();
                    return PeerAddress.fromIp(("127.0.0.1") + ":" + this.networkListener.getListenAddress().getPort());
            } else {
                return PeerAddress.fromIp("127.0.0.1:0");
            }
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


    // We register this class to LISTEN for some events that might be published by other handlers.
    private void registerForEvents() {
        eventBus.subscribe(DisconnectPeerRequest.class, e -> onDisconnectPeerRequest((DisconnectPeerRequest) e));
        eventBus.subscribe(ConnectPeerRequest.class, e -> onConnectPeerRequest((ConnectPeerRequest) e));
        eventBus.subscribe(ConnectPeersRequest.class, e -> onConnectPeersRequest((ConnectPeersRequest) e));
        eventBus.subscribe(PeersBlacklistedEvent.class, e -> onPeersBlacklisted((PeersBlacklistedEvent) e));
        eventBus.subscribe(PeersRemovedFromBlacklistEvent.class, e -> onPeersRemovedFromBlacklist((PeersRemovedFromBlacklistEvent) e));
        eventBus.subscribe(ResumeConnectingRequest.class, e -> onResumeConnecting((ResumeConnectingRequest) e));
        eventBus.subscribe(StopConnectingRequest.class, e -> onStopConnecting((StopConnectingRequest) e));
        eventBus.subscribe(DisconnectPeersRequest.class, e -> onDisconnectPeers((DisconnectPeersRequest) e));
    }

    // Event Handlers:
    private void onDisconnectPeerRequest(DisconnectPeerRequest request) {
        logger.trace("DisconnectPeerRequest received: {}", request.getPeerAddress());
        this.disconnect(request.getPeerAddress());
    }

    private void onConnectPeerRequest(ConnectPeerRequest request)        { this.connect(request.getPeerAddres()); }
    private void onConnectPeersRequest(ConnectPeersRequest request)      { this.connect(request.getPeerAddressList()); }
    private void onPeersBlacklisted(PeersBlacklistedEvent event)         { this.blacklist(event.getInetAddresses());}
    private void onPeersRemovedFromBlacklist(PeersRemovedFromBlacklistEvent event)
    { this.removeFromBlacklist(event.getInetAddresses());}

    private void onResumeConnecting(ResumeConnectingRequest request) {
//        if (super.state().equals(Service.State.STARTING) || super.state().equals(Service.State.STOPPING)) return;
//        resumeConnecting();
    }

    private void onStopConnecting(StopConnectingRequest request) {
//        if (super.state().equals(Service.State.STARTING) || super.state().equals(Service.State.STOPPING)) return;
//        stopConnecting();
    }

    private void onDisconnectPeers(DisconnectPeersRequest request) {
        if (request.getPeersToDisconnect() != null) {
            this.disconnect(request.getPeersToDisconnect());
        }
        if (request.getPeersToKeep() != null) {
            this.disconnectAllExcept(request.getPeersToKeep());
        }
    }

    private void processDisconnectRequest(DisconnectPeerRequest request) {
        processDisconnectRequests(Collections.singletonList(request));
    }

    private void processDisconnectRequests(List<DisconnectPeerRequest> requests) {
        if (requests == null) return;
        List<DisconnectPeerRequest> newList = requests.stream()
                .filter(r -> !pendingToCloseConns.contains( new PeerAddress2DisconnectPeerRequest_Comparator(r.getPeerAddress()) ))
                .filter(r -> (activeConns.containsKey(r.getPeerAddress()) || pendingToOpenConns.contains(r.getPeerAddress())))
                .toList();
        if (newList.size() > 0) {
            logger.trace("{} : Registering " + newList.size() + " Peers for Disconnection...", this.id);
            pendingToCloseConns.addAll(newList);
            pendingToOpenConns.removeAll(newList.stream().map(DisconnectPeerRequest::getPeerAddress).toList());
            eventsToProcess.release();
        }
    }

    private void disconnectAllExcept(List<PeerAddress> peerAddresses) {
        Predicate<PeerAddress> notToRemove = p -> !peerAddresses.contains(p);
        List<DisconnectPeerRequest> activePeersToRemove = activeConns.keySet()
                .stream()
                .filter(notToRemove)
                .map(p -> new DisconnectPeerRequest(p, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL))
                .collect(Collectors.toList());
        processDisconnectRequests(activePeersToRemove);
    }

    /**
     * Handle the pending connections to open. For each PeerAddress, it tries to open a Socket channel. If
     * there is a limit in the maximum number of connections and we reach it, it does nothing.
     */
    private void handlePendingToOpenConnections() {
        // loop over the pending Connections...
        while (true) {
            if (activeConns.size() >= networkConfig.getMaxSocketConnections()) break;

            PeerAddress peerAddress = this.pendingToOpenConns.poll();
            if (peerAddress == null) break;     // No more pending connections to open.

            if (!shouldProcessPeer(peerAddress)) {
                continue;
            }

            logger.trace("{} : {} : opening connection. Still pendingToOpen in Queue: {}, Active: {} ", this.id, peerAddress, this.pendingToOpenConns.size(), this.activeConns.size());
            openConnection(peerAddress);
        }
    }

    private boolean shouldProcessPeer(PeerAddress peerAddress) {
        boolean processThisPeer;

        processThisPeer = (peerAddress != null);
        processThisPeer &= (!activeConns.containsKey(peerAddress));
        processThisPeer &= (!blacklist.contains(peerAddress.getIp()));

        return processThisPeer;
    }

    /**
     * Find a NetworkController to open and handle the connection.
     */
    private void openConnection(PeerAddress peerAddress) {
        // at the moment we only have one NetworkController
        networkController.openConnection(peerAddress);
        activeConns.put(peerAddress, networkController);
    }

    /**
     * Handles the connections pending to close.
     */
    private void handlePendingToCloseConnections() {
        DisconnectPeerRequest disconnectRequest;
        while ((disconnectRequest = pendingToCloseConns.poll()) != null) {
            PeerAddress peerAddress = disconnectRequest.getPeerAddress();
            NetworkController networkController = activeConns.get(peerAddress);
            if (networkController != null) {
                logger.trace("{} : {} : Closing connection...", this.id, peerAddress);
                networkController.closeConnection(peerAddress, disconnectRequest.getReason());
                activeConns.remove(peerAddress);
            }
        }
    }

    /**
     * Helper used to search if pendingToCloseConns contains a disconnect request for a specific peer
     */
    private static class PeerAddress2DisconnectPeerRequest_Comparator {
        private final PeerAddress peerAddress;
        public PeerAddress2DisconnectPeerRequest_Comparator(PeerAddress peerAddress) {
            this.peerAddress = peerAddress;
        }
        @Override
        public boolean equals(final Object o) {
            final var other = (DisconnectPeerRequest) o;
            assert other!=null; // should never be called with any other type of object
            return peerAddress.equals(other.getPeerAddress());
        }
    }

    // Convenience method to get a reference to a P2PBuilder
    public static P2PBuilder builder(String id) {
        return new P2PBuilder(id);
    }

    public RuntimeConfig getRuntimeConfig()     { return this.runtimeConfig; }
    public P2PConfig getNetworkConfig()         { return this.networkConfig; }
    public ProtocolConfig getProtocolConfig()   { return this.protocolConfig; }
    public EventBus getEventBus()               { return this.eventBus;}
}