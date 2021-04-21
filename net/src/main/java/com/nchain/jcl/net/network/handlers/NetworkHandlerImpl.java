package com.nchain.jcl.net.network.handlers;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.config.NetworkConfig;
import com.nchain.jcl.net.network.config.NetworkConfigImpl;
import com.nchain.jcl.net.network.events.*;

import com.nchain.jcl.net.network.streams.StreamCloseEvent;
import com.nchain.jcl.net.network.streams.nio.NIOInputStream;
import com.nchain.jcl.net.network.streams.nio.NIOOutputStream;
import com.nchain.jcl.net.network.streams.nio.NIOStream;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.events.EventBus;
import com.nchain.jcl.tools.files.FileUtils;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import com.nchain.jcl.tools.log.LoggerUtil;

import com.nchain.jcl.tools.thread.ThreadUtils;
import com.nchain.jcl.tools.thread.TimeoutTask;
import com.nchain.jcl.tools.thread.TimeoutTaskBuilder;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of the NetworkHandler, based on Java-NIO (non blocking Input-Output)
 * - The class runs in a separate Thread, extending a guava Service
 * - the main loop is performed in a single Thread. This loop takes place in the "run()" method and basically
 *   loops over the Keys in our Selector, waiting for some event (a new peer connecting, new data coming, etc).
 * - Any time a new connection arrives, an isntance of a NIO Stream is linked to that Key, and an Event is
 *   triggered containing that Stream, that will be used to communicate with the remote Peer.
 *
 * - This class keeps different list ot keep track of the Pers to connect to or the ones to disconnect from. These
 *   lists are processed in another 2 different Threads, one each.
 *
 *
 */
public class NetworkHandlerImpl extends AbstractExecutionThreadService implements NetworkHandler {

    /** Subfolder to store local files in */
    private static final String NET_FOLDER = "net";

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    /**
     * Inner class that is attached to each Key in the selector. It represents a connection with
     * one particular Peer:
     * - when the connection is crated in the first place (but not fully established yet), the "peerAddress" stores the
     *   peer we are trying to connect to
     * - When the connection is fully established, the "stream" is created and linked to this socket.
     */
    class KeyConnectionAttach {
        PeerAddress peerAddress;
        NIOStream stream;
        public KeyConnectionAttach(PeerAddress peerAddress) { this.peerAddress = peerAddress;}
    }

    /**
     * Inner class that represents a connection that is in progress: We managed to connect to the remote Peer, but we
     * have not received a Confirmation from their end to establish the connection
     */
    class InProgressConn {
        PeerAddress peerAddress;
        long connTimestamp; // timestamp when the connection was triggered from our end
        public InProgressConn(PeerAddress peerAddress) {
            this.peerAddress = peerAddress;
            this.connTimestamp = System.currentTimeMillis();
        }
        public boolean hasExpired(long refTimestamp) {
            return ((System.currentTimeMillis() - connTimestamp) > refTimestamp);
        }
    }

    // Basic Attributes:
    protected String id;
    protected RuntimeConfig runtimeConfig;
    protected NetworkConfig config;
    protected Selector selector;

    // Main Lock, to preserve Thread safety and our mental sanity:
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    // For logging:
    private LoggerUtil logger;

    // Local Address of this Handler running:
    private PeerAddress peerAddress;

    // Indicates if we are running in SERVER_MODE (incoming connections allowed)
    private boolean server_mode = false;

    // Indicates if we can keep creating connections or we should stop:
    private boolean keep_connecting = true;

    // The EventBus used for event handling
    private EventBus eventBus;

    // An executor Service, to trigger jobs in MultiThread...
    ExecutorService jobExecutor = ThreadUtils.getThreadPoolExecutorService(HANDLER_ID + "-");

    // General State:
    private NetworkHandlerState state;

    // The following lists manage the different workingState the connections go though:
    // active:          The connection is established to a Remote Peer. Ready to send/receive data from it
    // inProgress:      The connection is yet to be confirmed by the Remote Peer
    // pendingToOpen:   List of new Connections we are opening to more Remote Peers
    // pendingToClose:  List of Peers which connections we are closing
    // blacklist:       List of Peers blacklisted

    private Map<PeerAddress, NIOStream> activeConns = new ConcurrentHashMap<>();
    private Map<PeerAddress, InProgressConn> inProgressConns = new ConcurrentHashMap<>();
    private BlockingQueue<PeerAddress> pendingToOpenConns = new LinkedBlockingQueue<>();
    private BlockingQueue<PeerAddress> pendingToCloseConns = new LinkedBlockingQueue<>();
    private Set<InetAddress> blacklist = ConcurrentHashMap.newKeySet();
    private Set<PeerAddress> failedConns = ConcurrentHashMap.newKeySet();

    // Other useful counters:
    private AtomicLong numConnsFailed = new AtomicLong();
    private AtomicLong numConnsInProgressExpired = new AtomicLong();

    // Files to store info after the handler has stopped:
    private static final String FILE_ACTIVE_CONN            = "networkHandler-activeConnections.csv";
    private static final String FILE_IN_PROGRESS_CONN       = "networkHandler-inProgressConnections.csv";
    private static final String FILE_PENDING_OPEN_CONN      = "networkHandler-pendingToOpenConnections.csv";
    private static final String FILE_FAILED_CONN            = "networkHandler-failedConnections.csv";


    /** Constructor */
    public NetworkHandlerImpl(String id, RuntimeConfig runtimeConfig, NetworkConfig netConfig, PeerAddress localAddress) {
        this.id = id;
        this.runtimeConfig = runtimeConfig;
        this.config = netConfig;
        this.peerAddress = localAddress;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
    }

    @Override
    public HandlerConfig getConfig() {
        return (NetworkConfigImpl) config;
    }
    @Override
    public void useEventBus(EventBus eventBus)      { this.eventBus = eventBus; }
    @Override
    public void stopConnecting()                    { this.keep_connecting = false; }
    @Override
    public void resumeConnecting()                  { this.keep_connecting = true;}

    @Override
    public NetworkHandlerState getState() {
        NetworkHandlerState result = null;
        try {
            lock.readLock().lock();
            result = NetworkHandlerState.builder()
                    .numActiveConns(this.activeConns.size())
                    .numInProgressConns(this.inProgressConns.size())
                    .numPendingToCloseConns(this.pendingToCloseConns.size())
                    .numPendingToOpenConns(this.pendingToOpenConns.size())
                    .keep_connecting(this.keep_connecting)
                    .server_mode(this.server_mode)
                    .numConnsFailed(this.numConnsFailed.get())
                    .numInProgressConnsExpired(this.numConnsInProgressExpired.get())
                    .build();
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public void connect(PeerAddress peerAddress) {
        //System.out.println("Connecting to " + peerAddress + " peers...");
        connect(Arrays.asList(peerAddress));
    }

    @Override
    public synchronized void connect(List<PeerAddress> peerAddresses) {
        //System.out.println("Connecting t0 " + peerAddresses.size() + " peers...");
        if (peerAddresses == null) return;
        if (super.isRunning()) {
            try {
                lock.writeLock().lock();
                // First we remove the Peers we are already connected to, or in process to...
                List<PeerAddress> listToAdd = peerAddresses.stream()
                        .filter(p -> !inProgressConns.containsKey(p))
                        .filter(p -> !activeConns.containsKey(p))
                        .filter(p -> !pendingToOpenConns.contains(p))
                        .filter(p -> !pendingToCloseConns.contains(p))
                        .filter(p -> !blacklist.contains(p.getIp()))
                        .collect(Collectors.toList());

                if (listToAdd.size() > 0) {
                    // Now we check that we are not breaking the limit in the Pending Socket Connections:
                    // If there si no limit, we just include them all. If there is a limit, we only include them up
                    // to the limit.

                    List<PeerAddress> finalListToAdd = listToAdd;

                    if (config.getMaxSocketPendingConnections().isPresent()) {
                        int limit = config.getMaxSocketPendingConnections().getAsInt();
                        int numItemsToAdd = Math.min(finalListToAdd.size(), limit - pendingToOpenConns.size());
                        if (numItemsToAdd > 0)
                            finalListToAdd = listToAdd.subList(0, numItemsToAdd);
                        else finalListToAdd = new ArrayList<>(); // empty List
                    }
                    pendingToOpenConns.addAll(finalListToAdd);
                    selector.wakeup();
                } // if...
            } finally {
                lock.writeLock().unlock();
            }
        }
    }



    @Override
    public void disconnect(PeerAddress peerAddress) {
        disconnect(Arrays.asList(peerAddress));
    }

    @Override
    public void disconnect(List<PeerAddress> peerAddresses) {
        if (peerAddresses == null) return;

        if (super.isRunning()) {
            try {
                lock.writeLock().lock();
                List<PeerAddress> newList = peerAddresses.stream()
                        .filter(p -> !pendingToCloseConns.contains(p))
                        .filter(p -> (activeConns.containsKey(p) || pendingToOpenConns.contains(p)))
                        .collect(Collectors.toList());
                if (newList.size() > 0) {
                    logger.trace( "Registering " + newList.size() + " Peers for Disconnection...");
                    pendingToCloseConns.addAll(newList);
                    pendingToOpenConns.removeAll(newList);
                    selector.wakeup();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void disconnectAllExcept(List<PeerAddress> peerAddresses) {
        try {
            lock.writeLock().lock();
            Predicate<PeerAddress> notToRemove = p -> !peerAddresses.contains(p);
            List<PeerAddress> activePeersToRemove = activeConns.keySet().stream().filter(notToRemove).collect(Collectors.toList());
            disconnect(activePeersToRemove);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void blacklist(InetAddress ipAddress, PeersBlacklistedEvent.BlacklistReason reason) {
        Map<InetAddress, PeersBlacklistedEvent.BlacklistReason> map = new HashMap<>();
        map.put(ipAddress, reason);
        blacklist(map);
    }

    public void blacklist(Map<InetAddress, PeersBlacklistedEvent.BlacklistReason> ipAddresses) {
        if (ipAddresses == null) return;
        try {
            lock.writeLock().lock();

            // First, we addBytes the IpAddress to the Blacklist, to keep a reference to them.
            blacklist.addAll(ipAddresses.keySet());

            // Then, we disconnect all the current Peers already connected to any of those addresses...
            List<PeerAddress> peersToDisconnect = this.activeConns.keySet().stream()
                    .filter(p -> ipAddresses.keySet().contains(p.getIp()))
                    .collect(Collectors.toList());

            this.disconnect(peersToDisconnect);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void whitelist(List<InetAddress> ipAddresses) {
        if (ipAddresses == null) return;
        try {
            lock.writeLock().lock();
            blacklist.removeAll(ipAddresses);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void init() {
        try {
            // We initialize the Handler:
            selector = SelectorProvider.provider().openSelector();

            // if we run in Server-Mode, we configure the Socket to be listening to incoming requests:
            if (server_mode) {
                SocketAddress serverSocketAddress = new InetSocketAddress(peerAddress.getIp(), peerAddress.getPort());
                ServerSocketChannel serverSocket = ServerSocketChannel.open();
                serverSocket.configureBlocking(false);
                serverSocket.socket().bind(serverSocketAddress);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT );

                // In case the local getPort is ZERO, that means that the system will pick one up for us, so we need to
                // update it after it's been assigned:
                if (peerAddress.getPort() == 0)
                    peerAddress = new PeerAddress(peerAddress.getIp(), serverSocket.socket().getLocalPort());
            }

            // Finally, we register for Events in the EventBus:
            registerForEvents();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // We register this calss to LISTEN for some Events that might be published by another Hndlers.
    public void registerForEvents() {
        eventBus.subscribe(DisconnectPeerRequest.class,     e -> onDisconnectPeerRequest((DisconnectPeerRequest) e));
        eventBus.subscribe(ConnectPeerRequest.class,        e -> onConnectPeerRequest((ConnectPeerRequest) e));
        eventBus.subscribe(ConnectPeersRequest.class,       e -> onConnectPeersRequest((ConnectPeersRequest) e));
        eventBus.subscribe(PeersBlacklistedEvent.class,     e -> onPeersBlacklisted((PeersBlacklistedEvent) e));
        eventBus.subscribe(PeersWhitelistedEvent.class,     e -> onPeersWhitelisted((PeersWhitelistedEvent) e));
        eventBus.subscribe(ResumeConnectingRequest.class,   e -> onResumeConnecting((ResumeConnectingRequest) e));
        eventBus.subscribe(StopConnectingRequest.class,     e -> onStopConnecting((StopConnectingRequest) e));
        eventBus.subscribe(DisconnectPeersRequest.class,    e -> onDisconnectPeers((DisconnectPeersRequest) e));
        eventBus.subscribe(BlacklistPeerRequest.class,      e -> onBlacklistPeer((BlacklistPeerRequest) e));
    }

    // Event Handlers:
    private void onDisconnectPeerRequest(DisconnectPeerRequest request)  { this.disconnect(request.getPeerAddress()); }
    private void onConnectPeerRequest(ConnectPeerRequest request)        { this.connect(request.getPeerAddres()); }
    private void onConnectPeersRequest(ConnectPeersRequest request)      { this.connect(request.getPeerAddressList()); }
    private void onPeersBlacklisted(PeersBlacklistedEvent event)         { this.blacklist(event.getInetAddresses());}
    private void onPeersWhitelisted(PeersWhitelistedEvent event)         { this.whitelist(event.getInetAddresses());}

    private void onResumeConnecting(ResumeConnectingRequest request) {
        if (super.state().equals(State.STARTING) || super.state().equals(State.STOPPING)) return;
        resumeConnecting();

    }

    private void onStopConnecting(StopConnectingRequest request) {
        if (super.state().equals(State.STARTING) || super.state().equals(State.STOPPING)) return;
        stopConnecting();
    }

    private void onDisconnectPeers(DisconnectPeersRequest request) {
        this.disconnect(request.getPeersToDisconnect());
        this.disconnectAllExcept(request.getPeersToKeep());
    }

    private void onBlacklistPeer(BlacklistPeerRequest request) {
        this.blacklist(request.getPeerAddress().getIp(), PeersBlacklistedEvent.BlacklistReason.CLIENT);
    }

    @Override
    public void start() {
        checkState(!super.isRunning(), "The Service is already Running");
        try {
            init();
            super.startAsync();
            super.awaitRunning();
            // We publish the event so you can notified when the Network stuff is started:
            eventBus.publish(new NetStartEvent(this.peerAddress));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error starting the service");
        }
    }

    @Override
    public void startServer() {
        this.server_mode = true;
        start();
    }

    /**
     * Main Loop of this class. It keeps running in the background until the instance is stopped by calling the "stop"
     * method.
     */
    @Override
    public void run() {
        logger.info("starting in " + (server_mode? "SERVER" : "CLIENT") + " mode...");
        startConnectionsJobs();
        try {
            while (isRunning()) {
                handleSelectorKeys(selector);
                //logger.trace("TEsting...");
            }
        } catch (Throwable e) {
            logger.error(e, "Error running the NetworkHandlerImpl");
            e.printStackTrace();
        } finally {
            stopConnectionsJobs();
            closeAllKeys(selector);
        }
    }

    @Override
    public void stop() {
        try {
            logger.info("Stopping...");
            // We save the Network Activity...
            saveNetworkActivity();

            // We publish the event so you can notified when the Network stuff is stopped:
            eventBus.publish(new NetStopEvent());
            selector.wakeup();
            super.stopAsync();
            super.awaitTerminated(5_000, TimeUnit.MILLISECONDS);

        } catch (TimeoutException te) {
            //te.printStackTrace();
            logger.error("Timeout while Waiting for the Service to Stop. Stopping anyway...");
        } catch (Exception e) {
            logger.error(e, "Error stopping the service ");
        }
    }

    // It saves the network activity into CSV files on disk. It saves the content of all of the List managed by this
    // handler (open connections, pending, etc), so they can be verified after the execution is over. if needed.
    private void saveNetworkActivity() {

        logger.debug( "Storing network activity to disk...");

        FileUtils fileUtils = runtimeConfig.getFileUtils();
        // Saving Active Connections
        Path filePath = Paths.get(fileUtils.getRootPath().toString(), NET_FOLDER, FILE_ACTIVE_CONN);
        fileUtils.writeCSV(filePath, this.activeConns.keySet());

        // Saving In progress Connections:
        filePath = Paths.get(fileUtils.getRootPath().toString(), NET_FOLDER, FILE_IN_PROGRESS_CONN);
        fileUtils.writeCSV(filePath, this.inProgressConns.keySet());

        // Saving pending To open connections:
        filePath = Paths.get(fileUtils.getRootPath().toString(), NET_FOLDER, FILE_PENDING_OPEN_CONN);
        fileUtils.writeCSV(filePath, this.inProgressConns.keySet());

        // Saving rejected Connections:
        filePath = Paths.get(fileUtils.getRootPath().toString(), NET_FOLDER, FILE_FAILED_CONN);
        fileUtils.writeCSV(filePath, this.failedConns);
    }

    /** Processes the pending Connections in a separate Thread */
    private void startConnectionsJobs() {
        jobExecutor.submit(this::handlePendingToOpenConnections);
        jobExecutor.submit(this::handlePendingToCloseConnections);
        jobExecutor.submit(this::handleInProgressConnections);
    }

    /** Stops the processing of pending Connections (running in a separate Thread) */
    private void stopConnectionsJobs() {
        if (jobExecutor != null) jobExecutor.shutdown();
    }

    /**
     * Logic to execute when a Connection to a Remote Peer has failed, so there is actually no connection at all.
     * We just discard and blacklist this Peer.
     */
    private void processConnectionFailed(PeerAddress peerAddress,
                                         PeerRejectedEvent.RejectedReason reason,
                                         String detail) {
        logger.trace(peerAddress, "Processing connection Failed :: " + reason + " : " + detail);
        try {
            lock.writeLock().lock();

            logger.trace(peerAddress, reason.name(), detail);
            failedConns.add(peerAddress);
            inProgressConns.remove(peerAddress);
            numConnsFailed.incrementAndGet();
            blacklist(peerAddress.getIp(), PeersBlacklistedEvent.BlacklistReason.CONNECTION_REJECTED);

            // We publish the event
            eventBus.publish(new PeerRejectedEvent(peerAddress, reason, detail));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * It initializes a new connection to one Peer, creating a ByteArrayStream representing that connection and
     * triggering the callback, sending back the reference to that stream back to the client.
     * @param key       SelectionKey related to this Socket/Channel
     */
    protected void startPeerConnection(SelectionKey key) throws IOException {

        try {
            lock.writeLock().lock();

            KeyConnectionAttach keyAttach = (KeyConnectionAttach) key.attachment();

            // We create the NIOStream and link it to this key (as attachment):
            NIOStream stream = new NIOStream(
                    keyAttach.peerAddress,
                    ThreadUtils.PEER_STREAM_EXECUTOR,
                    this.runtimeConfig,
                    this.config,
                    key);
            stream.init();
            keyAttach.stream = stream;

            // We addBytes this connection to the list of active ones (not "in Progress" anymore):
            inProgressConns.remove(keyAttach.peerAddress);
            activeConns.put(keyAttach.peerAddress, stream);
            logger.debug(keyAttach.peerAddress, "Connection established.");

            // We trigger the callbacks, sending the Stream back to the client:
            eventBus.publish(new PeerConnectedEvent(keyAttach.peerAddress));
            eventBus.publish(new PeerNIOStreamConnectedEvent(stream));

            // From now moving forward, this key is ready to READ data:
            key.interestOps((key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * It handles one of the "pendingToOpen" connections. It opens the connection and registers the Key in the
     * selector. If the connection process takes longer than the limit workingState in the configuration, then we discard
     * this Peer.
     * @param peerAddress Peer to connect to
     */
    private void handleConnectionToOpen(PeerAddress peerAddress) {
        try {
            lock.writeLock().lock();
            logger.trace(peerAddress, "Connecting...");
            inProgressConns.put(peerAddress, new InProgressConn(peerAddress));

            SocketAddress socketAddress = new InetSocketAddress(peerAddress.getIp(), peerAddress.getPort());
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            boolean isConnected = socketChannel.connect(socketAddress);
            SelectionKey key = (isConnected)
                    ? socketChannel.register(selector, SelectionKey.OP_READ)
                    : socketChannel.register(selector, SelectionKey.OP_CONNECT);

            key.attach(new KeyConnectionAttach(peerAddress));


            if (isConnected) {
                logger.trace(peerAddress, "Connected, establishing connection...");
                startPeerConnection(key);

            } else {
                logger.trace(peerAddress, "Connected, waiting for remote confirmation...");
            }
            this.selector.wakeup();

        } catch (Exception e) {
            //e.printStackTrace();
            processConnectionFailed(peerAddress, PeerRejectedEvent.RejectedReason.INTERNAL_ERROR, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * It handles the pending Connections to Open. For each PeerAddress, it tries to open a Socket channel. If
     * there is a limit in the maximum number of Connections and we reach it, it does nothing.
     */
    private void handlePendingToOpenConnections() {
        try {
            // First loop level: This job keeps on forever...
            while (true) {
                // We set the limit of connections (Sockets), if any. the number of "inProgress" + "active" connections
                // cannot be higher than this value.
                OptionalInt limitNumConns = config.getMaxSocketConnections();

                // Second loop level: We loop over the pending Connections...
                while (true) {
                    // Basic checks before getting a Peer from the Pool:
                        // If any of these checks fail, we break the loop (we don't process any more peers)
                        if (!this.selector.isOpen()) break;
                        if (!keep_connecting) break;
                        if (inProgressConns.size() > config.getMaxSocketConnectionsOpeningAtSameTime()) break;
                        if ((limitNumConns.isPresent()) && (inProgressConns.size() + activeConns.size() >= limitNumConns.getAsInt())) break;

                        // Basic checks after obtaining the Peer from the Pool:
                        // If any of these checks fail, we just skip to the next Peer
                        PeerAddress peerAddress = this.pendingToOpenConns.take();
                        if (peerAddress == null) continue;

                        // We handle this connection.
                        // In case opening the connection takes too long, we wrap it up in a TimeoutTask...

                        //System.out.println(" >>>>> CONNECTING TO " + peerAddress.toString() + ", " + Thread.activeCount() + " Threads, " + pendingToOpenConns.size() + " pendingToOpen Conns");
                        TimeoutTask connectPeerTask = TimeoutTaskBuilder.newTask()
                                .execute(() -> handleConnectionToOpen(peerAddress))
                                .waitFor(config.getTimeoutSocketConnection().getAsInt())
                                .ifTimeoutThenExecute(() -> {
                                            processConnectionFailed(peerAddress, PeerRejectedEvent.RejectedReason.TIMEOUT,"connection timeout");
                                            //System.out.println("<<<<< CONNECTION TIMEOUT " + peerAddress.toString() + ", " + Thread.activeCount() + " Threads");
                                        }
                                )
                                .build();
                        connectPeerTask.execute();

                        if (activeConns.containsKey(peerAddress)) continue;
                        if (inProgressConns.containsKey(peerAddress)) continue;
                        if (blacklist.contains(peerAddress.getIp())) continue;
                } // while...

                // In case there are NO more connections pending to Open, We wait until the Queue of Pending
                // connection has some content, or we are allowed to keep making  connections..
                while (pendingToOpenConns.size() == 0 || !this.keep_connecting) Thread.sleep(1000);

                Thread.sleep(1000); // To avoid tight loops and CPU overload

            } // while...
        } catch (Throwable th) {
            th.printStackTrace();
            //handlerLogger.error(th, th.getMessage());
        }

    }

    /**
     * It handles the in-progress connections. these connections are already open from our end, but we are just waiting
     * for the remote Peer to confirm (through a CONNECT Key in the KeySelector). This method loops over all these
     * connections and remove those ones that are expired based on our config (timeoutSocketRemoteConfirmation)
     * NOTE: An expired and remove connection from there might still confirm later on, sending a CONNECT signal to us. In
     * that case, the connection is still accepted and inserted into the "active" conns.
     */
    private void handleInProgressConnections() {
        try {
            // First loop level: This job keeps on forever...
            // We keep a temporary list where we keep a reference to those In-Progress Connections that need to be removed
            // because they have expired...
            List<PeerAddress> inProgressConnsToRemove = new ArrayList<>();
            while (true) {
                // Second loop level: We loop over the InProgress Connections...
                try {
                    lock.writeLock().lock();
                    for (PeerAddress peerAddress : this.inProgressConns.keySet()) {
                        InProgressConn inProgressConn = this.inProgressConns.get(peerAddress);
                        if (inProgressConn.hasExpired(this.config.getTimeoutSocketRemoteConfirmation().getAsInt())) {
                            inProgressConnsToRemove.add(peerAddress);
                        }
                    } // for...
                    // we remove the expired connections...
                    if (!inProgressConnsToRemove.isEmpty()) {
                        logger.debug("Removing " + inProgressConnsToRemove.size() + " in-progress expired connections");
                        numConnsInProgressExpired.addAndGet(inProgressConnsToRemove.size());
                        inProgressConnsToRemove.forEach(p -> inProgressConns.remove(p));
                        inProgressConnsToRemove.clear();
                    }
                } finally {
                    lock.writeLock().unlock();
                }
                Thread.sleep(1000); // avoid tight loops
            } // while...
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            throw new RuntimeException(ie);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    /**
     * It handles the connections pending to close.
     */
    private void handlePendingToCloseConnections() {

        try {
            // First loop level: This job keeps on forever...
            while (true) {
                PeerAddress peerAddress;
                while ((peerAddress = pendingToCloseConns.take()) != null) {
                    logger.trace(peerAddress, "Processing request to Close...");

                    // For each connection to closeAndClear, we check that we have already a SelectionKey for it.
                    // If we do, we check the Key, and put back the connection into the "PendingToOpen" Pool...

                    Iterator<SelectionKey> keys = selector.keys().iterator();
                    boolean found = false;
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();

                        if (key.attachment() != null) {
                            KeyConnectionAttach keyAttach = (KeyConnectionAttach) key.attachment();
                            if (peerAddress.equals(keyAttach.peerAddress)) {
                                logger.trace(peerAddress, "Removing Key... ");
                                closeKey(key, PeerDisconnectedEvent.DisconnectedReason.UNDEFINED);
                                // The Peer is sent back to the pool of connections to Open, so it can be reused later on
                                // TODO: DISABLED!!!!
                                //connect(keyAttach.peerAddress); // back to the Pool
                                found = true;
                            }
                        }
                    } // while...

                    // if we do NOT have a Selection Key for this Peer, that means that the Selection Key has been lost
                    // somehow. In this case at least we publish the Event...
                    if (!found) {
                        logger.trace(peerAddress, "Closing dead connection...", getState());
                        eventBus.publish(new PeerDisconnectedEvent(peerAddress, PeerDisconnectedEvent.DisconnectedReason.UNDEFINED));
                    }

                } // while...

                // We wait until the Queue of Pending connection has some content...
                while (pendingToCloseConns.size() == 0) Thread.sleep(1000);
            } // while..
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * It closes a Selection Key. It triggers the "onClose" method in the Stream that wrapps up this connections, so
     * the client os notified, and then it cancels this selection Key.
     */

    private void closeKey(SelectionKey key, PeerDisconnectedEvent.DisconnectedReason reason) {
        KeyConnectionAttach keyConnection = (KeyConnectionAttach) key.attachment();
        try {
            lock.writeLock().lock();

            // First we cancel the Key, so no more data will come through this channel. Then, we closeAndClear the
            // the stream by invoking the "onClose" method...
            //key.interestOps(0);
            key.channel().close();
            key.cancel();

            if (keyConnection != null) {
                if (activeConns.containsKey(keyConnection.peerAddress)) {
                    if (keyConnection.stream != null) {
                        // We trigger the Close event down the Stream......
                        logger.trace(keyConnection.stream.getPeerAddress(), "Peer socket closed");
                        keyConnection.stream.input().close(new StreamCloseEvent());
                    }
                    pendingToCloseConns.remove(keyConnection.peerAddress);
                    inProgressConns.remove(keyConnection.peerAddress);
                    // We notify about this Peer being Disconnected:
                    eventBus.publish(new PeerDisconnectedEvent(keyConnection.peerAddress, reason));

                    activeConns.remove(keyConnection.peerAddress);
                    logger.trace(keyConnection.peerAddress, "Connection closed");
                }
                //failedConns.addBytes(keyConnection.peerAddress);
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e, "Error closing a Key");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * It performs a loop to handle the Selection Keys.
     */
    private void handleSelectorKeys(Selector selector) throws IOException, InterruptedException {
        selector.select();
        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            keyIterator.remove();
            handleKey(key);
        }
        // We addBytes a Delay, so more keys are accumulated on each iteration and we avoid tight loops:
        //Thread.sleep(50);
    }

    /**
     * It handles each selection key from the selector. This logic is encapsualted in this method, so it can be
     * override by a child class in case we don't want to habdle specific keys or handle new ones (like the
     * ACCEPT key, which s not handled here, since the NetworkHandlerImpl does not accept incoming connections)
     * @param key Key to handle
     */
    protected void handleKey(SelectionKey key) throws IOException {
        try {
        //logger.trace("Key : " + key);
            if (!key.isValid()) {
                handleInvalidKey(key);
                return;
            }
            if (key.isConnectable()) {
                //logger.trace( "Handling Connectable Key " + key + "...");
                handleConnect(key);
                return;
            }
            if (key.isReadable()) {
                //logger.trace( "Handling Readable Key " + key + "...");
                handleRead(key);
                return;
            }
            if (key.isWritable()) {
                //logger.trace( "Handling Writable Key " + key + "...");
                handleWrite(key);
                return;
            }
            if ((server_mode) && (key.isAcceptable())) {
                //logger.trace("Handling Acceptable Key " + key + "...");
                handleAccept(key);
                return;
            }

        } catch (Exception e) {
            //handlerLogger.error(e, e.getMessage());
            //e.printStackTrace();
        }
    }

    /**
     * It handles a CONNECT key, that is a confirmation that a connection to a remote Peer is successful.
     */
    protected void handleConnect(SelectionKey key) throws IOException {
        try {
            lock.writeLock().lock();
            KeyConnectionAttach keyConnection = (KeyConnectionAttach) key.attachment();

            // Whatever happens, we remove this Peer form the "inProgress" Connections:
            if (key.attachment() != null) {
                inProgressConns.remove(keyConnection.peerAddress);
            }

            // Check:
            // we accept the connection, unless this Peer is already register for disconnection, or the Handler
            // does not accept new connections anymore, or we've reached the Maximum Connections limit already:

            OptionalInt limitNumConns = config.getMaxSocketConnections();
            if (pendingToCloseConns.contains(keyConnection.peerAddress) ||
                    (!keep_connecting) ||
                    ((limitNumConns.isPresent()) && (inProgressConns.size() + activeConns.size() >= limitNumConns.getAsInt()))) {
                closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
                return;
            }

            // If we reach this far, we accept the connection:
            SocketChannel socketChannel = (SocketChannel) key.channel();
            if (socketChannel.finishConnect()) {
                 startPeerConnection(key);
            } else closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);

        } catch (ConnectException e) {
            processConnectionFailed(((KeyConnectionAttach) key.attachment()).peerAddress, PeerRejectedEvent.RejectedReason.INTERNAL_ERROR, e.getMessage());
            //throw e;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * It handles a READ key, that is some data is ready to read from one Connection. Internally, the Stream
     * representing this connection is used to read data from the Socket (and return it to the "client" by using
     * the "onData" method)
     */
    protected void handleRead(SelectionKey key) throws IOException {
        // We read the data from the Peer (through the Stream wrapped out around it) and we run the callbacks:
        //handlerLogger.log(Level.TRACE, "read key...");
        KeyConnectionAttach keyConnection = (KeyConnectionAttach) key.attachment();
        int numBytesRead = ((NIOInputStream)keyConnection.stream.input()).readFromSocket();
        //logger.trace(numBytesRead + " read from " + ((NIOInputStream) keyConnection.stream.input()).getPeerAddress().toString());
        if (numBytesRead == -1) {
            logger.trace(keyConnection.peerAddress, "Connection closed by the Remote Peer.");
            this.closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_REMOTE);
        }
        /*
        else {
            listenerExecutor.executePeerIncomingData(keyConnection.peerAddress, numBytesRead);

        }*/

    }

    /**
     * It handles a WRITE key, that is writing some data to the socket implementing that connection. Each connections
     * is representing by a ByteArrayStream, so we use it to write the data through the channel.
     */
    protected void handleWrite(SelectionKey key) throws IOException {
        // We write the data to the Peer (through the Stream wrapped out around it) and we run the callbacks:
        KeyConnectionAttach keyConnection = (KeyConnectionAttach) key.attachment();
        int numBytesWrite = ((NIOOutputStream) keyConnection.stream.output()).writeToSocket();
        //logger.trace(numBytesWrite + " written to " + ((NIOOutputStream) keyConnection.stream.output()).getPeerAddress().toString());
    }

    /**
     * It handles an invalid key, closing it.
     */
    protected void handleInvalidKey(SelectionKey key) throws IOException {
        closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_REMOTE);
    }

    /**
     * It handles a new incoming Connection from a Remote Peer. The logic to here is similar as with an outcoming
     * connection: We create a Stream that wraps up the connection and we attach it to this key, so it can be used
     * later to send/receive data from/to this peer.
     */
    private void handleAccept(SelectionKey key) throws IOException {

        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();

        logger.trace(socket.getRemoteSocketAddress(), "incoming Connection...");

        // Check:
        // We accept the incoming conn only if the server is in "Accepting new connections" mode
        if (!keep_connecting) {
            logger.trace(socket.getRemoteSocketAddress(), "discarding incoming connection (no more connections needed).");
            closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
            return;
        }

        // Check:
        // We accept the incoming connection only if the Host is NOT Blacklisted

        InetAddress peerIP = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress();
        if (blacklist.contains(peerIP)) {
            logger.trace(socket.getRemoteSocketAddress(), "discarding incoming connection (blacklisted).");
            closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
            return;
        }

        // Check:
        // We haven't broken the "Maximum Socket connections" limit:

        if ((!config.getMaxSocketConnections().isEmpty()) &&
                (activeConns.size() >= config.getMaxSocketConnections().getAsInt())) {
            logger.trace(socket.getRemoteSocketAddress(), "no more connections allowed ("
                    + config.getMaxSocketConnections().getAsInt() + ")");
            closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
            return;
        }

        // IF we reach this far, we accept the incoming connection:

        logger.trace(socket.getRemoteSocketAddress(), "accepting Connection...");

        SelectionKey clientKey = channel.register(this.selector, SelectionKey.OP_READ );
        PeerAddress peerAddress = new PeerAddress(socket.getInetAddress(), socket.getPort());
        clientKey.attach(new KeyConnectionAttach(peerAddress));

        // We activate the Connection straight away:
        startPeerConnection(clientKey);

    }

    /**
     * It closes all Selection Keys
     */
    private void closeAllKeys(Selector selector) {
        logger.trace("Closing all Keys");
        try {
            selector.wakeup();
            selector.keys().forEach(k -> closeKey(k, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL));
            selector.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.error(ioe, "Error closing Selector");
        }
    }

    // Used by the Guava Service. Provides the name for the Thread running this Handler
    @Override
    protected String serviceName() {
        return HANDLER_ID+ "-main";
    }

}
