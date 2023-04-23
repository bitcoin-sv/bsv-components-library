package io.bitcoinsv.bsvcl.net.network;

// @author i.fernandez@nchain.com
// Copyright (c) 2018-2023 Bitcoin Association

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import io.bitcoinsv.bsvcl.net.P2PConfig;
import io.bitcoinsv.bsvcl.net.network.events.*;

import io.bitcoinsv.bsvcl.net.network.streams.StreamCloseEvent;
import io.bitcoinsv.bsvcl.net.network.streams.nio.NIOInputStream;
import io.bitcoinsv.bsvcl.net.network.streams.nio.NIOOutputStream;
import io.bitcoinsv.bsvcl.net.network.streams.nio.NIOStream;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.common.events.EventBus;
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkState;

/**
 * Implementation of the NetworkHandler, based on Java-NIO (non blocking Input-Output)
 * - The class runs in a separate Thread, extending a guava Service
 * - the main loop is performed in a single Thread. This loop takes place in the "run()" method and basically
 *   loops over the Keys in our Selector, waiting for some event (a new peer connecting, new data coming, etc).
 * - Any time a new connection arrives, an instance of a NIO Stream is linked to that Key, and an Event is
 *   triggered containing that Stream, that will be used to communicate with the remote Peer.
 * - This class keeps different list to keep track of the peers to connect to or the ones to disconnect from. These
 *   lists are processed in another 2 different threads, one for each list.
 */
public class NetworkController extends AbstractExecutionThreadService {

    /** Subfolder to store local files in */
    private static final String NET_FOLDER = "net";

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    /**
     * Inner class that is attached to each Key in the selector. It represents a connection with
     * one particular Peer:
     * - when the connection is created in the first place (but not fully established yet), the "peerAddress" stores the
     *   peer we are trying to connect to
     * - When the connection is fully established, the "stream" is created and linked to this socket.
     */
    class KeyConnectionAttach {
        PeerAddress peerAddress;
        NIOStream stream;
        boolean started; // set to TRUE when we have received already some bytes from this Peer
        public KeyConnectionAttach(PeerAddress peerAddress) { this.peerAddress = peerAddress;}
    }

    /**
     * Inner class that represents a connection that is in progress: We managed to connect to the remote Peer, but we
     * have not received a confirmation from their end to establish the connection
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
    protected P2PConfig config;
    protected Selector mainSelector;

    // Main Lock, to preserve Thread safety and our mental sanity:
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // For logging:
    private final Logger logger = LoggerFactory.getLogger(NetworkController.class);

    // Local Address of this Handler running:
    private PeerAddress peerAddress;

    // Indicates if we are running in SERVER_MODE (incoming connections allowed)
    private boolean serverMode;

    // Indicates if we can keep creating connections or whether we should stop:
    private boolean keepConnecting = true;
    // The EventBus used for event handling
    private EventBus eventBus;
    // An executor Service, to trigger jobs in MultiThread...
    ExecutorService jobExecutor = ThreadUtils.getCachedThreadExecutorService("JclNetworkHandler");
    // An executor for triggering new Connections to remote Peers:
    ExecutorService newConnsExecutor;
    // the connections that are being opened
    private final Map<PeerAddress, NetworkController.InProgressConn> inProgressConns = new ConcurrentHashMap<>();
    // General State:
    private NetworkControllerState state;

    // The following lists manage the different workingState the connections go though:
    // active:          The connection is established to a Remote Peer. Ready to send/receive data from it
    // inProgress:      The connection is yet to be confirmed by the Remote Peer
    // pendingToOpen:   List of new Connections we are opening to more Remote Peers
    // pendingToClose:  List of Peers which connections we are closing
    // blacklist:       List of Peers blacklisted

    private final Map<PeerAddress, NIOStream> activeConns = new ConcurrentHashMap<>();

    // Other useful counters:
    private final AtomicLong numConnsFailed = new AtomicLong();
    private final AtomicLong numConnsInProgressExpired = new AtomicLong();
    private long numConnsTried;

    // Files to store info after the handler has stopped:
    private static final String FILE_ACTIVE_CONN            = "networkHandler-activeConnections.csv";
    private static final String FILE_IN_PROGRESS_CONN       = "networkHandler-inProgressConnections.csv";
    private static final String FILE_PENDING_OPEN_CONN      = "networkHandler-pendingToOpenConnections.csv";
    private static final String FILE_FAILED_CONN            = "networkHandler-failedConnections.csv";

    public NetworkController(String id, RuntimeConfig runtimeConfig, P2PConfig netConfig, PeerAddress localAddress, boolean serverMode) {
        this.id = id;
        this.runtimeConfig = runtimeConfig;
        this.config = netConfig;
        this.peerAddress = localAddress;
        this.serverMode = serverMode;
        this.newConnsExecutor = ThreadUtils.getFixedThreadExecutorService("JclNetworkHandlerRemoteConn", netConfig.getMaxSocketConnectionsOpeningAtSameTime());
    }

    public P2PConfig getConfig() {
        return config;
    }

    public synchronized void updateConfig(P2PConfig config) {
        this.config = config;
    }

    public void useEventBus(EventBus eventBus)      { this.eventBus = eventBus; }

    public void stopConnecting()                    { this.keepConnecting = false; }

    public void resumeConnecting()                  { this.keepConnecting = true;}

    /** open a connection to the peer */
    public void openConnection(PeerAddress peerAddress) {
        handleConnectionToOpen(peerAddress);
    }

    public void closeConnection(PeerAddress peerAddress) {
        closeConnection(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
    }

    /** close a connection to the peer */
    public void closeConnection(PeerAddress peerAddress, PeerDisconnectedEvent.DisconnectedReason reason) {
        if (selectorMap.containsKey(peerAddress)) {
            Iterator<SelectionKey> selectorKeys = selectorMap.get(peerAddress).keys().iterator();
            while (selectorKeys.hasNext()) {
                SelectionKey key = selectorKeys.next();
                if (key.attachment() != null) {
                    KeyConnectionAttach keyAttach = (KeyConnectionAttach) key.attachment();
                    if (peerAddress.equals(keyAttach.peerAddress)) {
                        logger.trace("{} : {} : Removing Key [specific selector]... ", this.id, peerAddress);
                        closeKey(key, reason);
                    }
                }
            }
        }
    }

    public NetworkControllerState getState() {
        NetworkControllerState result = null;
        try {
            lock.readLock().lock();
            result = NetworkControllerState.builder()
                    .numActiveConns(this.activeConns.size())
                    .keep_connecting(this.keepConnecting)
                    .server_mode(this.serverMode)
                    .numConnsFailed(this.numConnsFailed.get())
                    .numInProgressConnsExpired(this.numConnsInProgressExpired.get())
                    .numConnsTried(this.numConnsTried)
                    .build();
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    public void init() {
        try {
            // We initialize the Handler:
            mainSelector = SelectorProvider.provider().openSelector();

            // if we run in Server-Mode, we configure the Socket to be listening to incoming requests:
            if (serverMode) {
                SocketAddress serverSocketAddress = new InetSocketAddress(peerAddress.getIp(), peerAddress.getPort());
                ServerSocketChannel serverSocket = ServerSocketChannel.open();
                serverSocket.configureBlocking(false);
                serverSocket.socket().setReuseAddress(true);
                serverSocket.socket().bind(serverSocketAddress);
                serverSocket.register(mainSelector, SelectionKey.OP_ACCEPT );

                // In case the local getPort is ZERO, that means that the system will pick one up for us, so we need to
                // update it after it's been assigned:
                if (peerAddress.getPort() == 0)
                    peerAddress = new PeerAddress(peerAddress.getIp(), serverSocket.socket().getLocalPort());
            }

            // Finally, we register for Events in the EventBus:
//            registerForEvents();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


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
            logger.error("{} : Error starting the service", this.id);
        }
    }

    @Deprecated
    public void startServer() {
        this.serverMode = true;
        start();
    }

    /**
     * Main Loop of this class. It keeps running in the background until the instance is stopped by calling the "stop"
     * method.
     */
    @Override
    public void run() throws IOException, InterruptedException {
        logger.info("{} : Starting in " + (serverMode ? "SERVER" : "CLIENT") + " mode...", this.id);
        startConnectionsJobs();
        try {
            while (isRunning()) {
                handleSelectorKeys(mainSelector);
            }
        } catch (Throwable e) {
            logger.error("{} : Error running the NetworkHandlerImpl", this.id, e);
            e.printStackTrace();
            throw e;
        } finally {
            stopConnectionsJobs();
            closeAllKeys(mainSelector);
        }
    }

    public void stop() throws InterruptedException {
        logger.info("{} : Stopping...", this.id);

        // We publish the event so you can notified when the Network stuff is stopped:
        eventBus.publish(new NetStopEvent());

        // We close all our connections:
        this.keepConnecting = false;
        for(PeerAddress peerAddress : this.activeConns.keySet())
            this.closeConnection(peerAddress);

        mainSelector.wakeup();
        super.stopAsync();

        stopSelectorThreads();
    }

    public void awaitStopped() {
        super.awaitTerminated();
    }

    /** Processes the pending Connections in a separate Thread */
    private void startConnectionsJobs() {
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
        logger.trace("{} : {} : Processing connection Failed : {} : {}", this.id, peerAddress,  reason, detail);
        try {
            lock.writeLock().lock();

//            final int maxFailedConnectionAddressesToKeep = 10000;
//            if(failedConns.size()>=maxFailedConnectionAddressesToKeep) {
//                logger.warn("{} : List of peer addresses to which connection has failed has reached maximum allowed size ({})!. Half of addresses will be removed from the list.", this.id, maxFailedConnectionAddressesToKeep);
//                var it = failedConns.iterator();
//                while (true) {
//                    if(!it.hasNext()) break;
//                    it.next();
//                    if(!it.hasNext()) break;
//                    it.next();
//                    it.remove();
//                }
//            }
//            // TODO: We should ban the peer that sent us too many ADDR messages containing an address to which the connection failed.
//            // TODO: Tracking addresses of peers to which the connection failed should be redesigned (maybe even removed), because currently these addresses are not used anywhere and are only written to file when NetworkHandler is stopped.
//            failedConns.add(peerAddress);
//            inProgressConns.remove(peerAddress);
//            numConnsFailed.incrementAndGet();
//            //blacklist(peerAddress.getIp(), PeersBlacklistedEvent.BlacklistReason.CONNECTION_REJECTED);

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
    private void startPeerConnection(SelectionKey key) throws IOException {

        try {
            lock.writeLock().lock();

            KeyConnectionAttach keyAttach = (KeyConnectionAttach) key.attachment();

            logger.info("{} : {} : Starting Connection... ", this.id, keyAttach.peerAddress);
            // We create the NIOStream and link it to this key (as attachment):
            NIOStream stream = new NIOStream(
                    keyAttach.peerAddress,
                    this.runtimeConfig,
                    this.config,
                    key);
            stream.init();
            keyAttach.stream = stream;

            // We add this connection to the list of active ones (not "in Progress" anymore):
            inProgressConns.remove(keyAttach.peerAddress);
            activeConns.put(keyAttach.peerAddress, stream);
            logger.trace("{} : {} : Socket connection established.", this.id, keyAttach.peerAddress);

            // We trigger the callbacks, sending the Stream back to the client:
            eventBus.publish(new PeerConnectedEvent(keyAttach.peerAddress));

            eventBus.publish(new PeerNIOStreamConnectedEvent(stream));

            // From now moving forward, this key is ready to READ data:
            key.interestOps((key.interestOps() | SelectionKey.OP_READ | SelectionKey.OP_WRITE) & ~SelectionKey.OP_CONNECT);
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
            numConnsTried++;
            logger.debug("{} : {} : Connecting...", this.id, peerAddress);
            inProgressConns.put(peerAddress, new InProgressConn(peerAddress));

            SocketAddress socketAddress = new InetSocketAddress(peerAddress.getIp(), peerAddress.getPort());
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            boolean isConnected = socketChannel.connect(socketAddress);
            var selector = SelectorProvider.provider().openSelector();
            registerAndRunSelector(peerAddress, selector);

            SelectionKey key = (isConnected)
                    ? socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE)
                    : socketChannel.register(selector, SelectionKey.OP_CONNECT);

            key.attach(new KeyConnectionAttach(peerAddress));

            if (isConnected) {
                logger.trace("{} : {} : Connected, establishing connection...", this.id, peerAddress);
                startPeerConnection(key);

            } else {
                logger.trace("{} : {} : Connected, waiting for remote confirmation...", this.id, peerAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
            processConnectionFailed(peerAddress, PeerRejectedEvent.RejectedReason.INTERNAL_ERROR, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Handle the in-progress connections. These connections have been opened from our end, but we are just waiting
     * for the remote Peer to confirm (through a CONNECT Key in the KeySelector). This method loops over all these
     * connections and remove those that are expired based on our config (timeoutSocketRemoteConfirmation)
     * NOTE: An expired and remove connection from there might still confirm later on, sending a CONNECT signal to us. In
     * that case, the connection is still accepted and inserted into the "active" connections.
     * todo: make sure that the P2P parent is notified of any disconnects.
     */
    private void handleInProgressConnections() {
        // We keep a temporary list where we keep a reference to those In-Progress Connections that need to be removed
        // because they have expired...
        List<PeerAddress> inProgressConnsToRemove = new ArrayList<>();
        // loop over the InProgress Connections...
        for (PeerAddress peerAddress : this.inProgressConns.keySet()) {
            NetworkController.InProgressConn inProgressConn = this.inProgressConns.get(peerAddress);
            if (inProgressConn.hasExpired(this.config.getTimeoutSocketRemoteConfirmation())) {
                inProgressConnsToRemove.add(peerAddress);
            }
        } // for...
        // we remove the expired connections...
        if (!inProgressConnsToRemove.isEmpty()) {
            logger.trace("{} : Removing in-progress expired connections", this.id, inProgressConnsToRemove.size());
            numConnsInProgressExpired.addAndGet(inProgressConnsToRemove.size());
            inProgressConnsToRemove.forEach(p -> inProgressConns.remove(p));
            inProgressConnsToRemove.clear();
        }
    }

    private void notifyPeerDisconnection(PeerAddress peerAddress, PeerDisconnectedEvent.DisconnectedReason reason) {
        if (this.peerAddress.equals(peerAddress)) return;
        eventBus.publish(new PeerDisconnectedEvent(peerAddress, reason));
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
                        logger.trace("{} : {} : Peer socket closed", this.id, keyConnection.stream.getPeerAddress());
                        keyConnection.stream.input().close(new StreamCloseEvent());
                    }
                    inProgressConns.remove(keyConnection.peerAddress);
                    activeConns.remove(keyConnection.peerAddress);
                    notifyPeerDisconnection(keyConnection.peerAddress, reason);
                    logger.trace("{} : {} : Connection closed", this.id, keyConnection.peerAddress);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("{} : Error closing a Key", this.id, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean checkDuplicatedConnection(SelectionKey key, PeerAddress peerAddress) throws IOException {
        // Check:
        // We don't process it if we are already connected to it:
        if (this.activeConns.containsKey(peerAddress)) {
            logger.trace("{} : {} : Received CONNECT Key for already existing connection. Ignoring.", this.id, peerAddress);

            key.channel().close();
            key.cancel();

            return true;
        }

        return false;
    }

    /**
     * It performs a loop to handle the Selection Keys.
     */
    private void handleSelectorKeys(Selector selector) throws IOException, InterruptedException {
        selector.select(100);   // we need a timeout, otherwise it will block forever...
        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            keyIterator.remove();
            handleKey(key);
        }
    }

    /**
     * It handles each selection key from the selector. This logic is encapsulated in this method, so it can be
     * overridden by a child class in case we don't want to handle specific keys or handle new ones (like the
     * ACCEPT key, which is not handled here since the NetworkHandlerImpl does not accept incoming connections)
     * @param key Key to handle
     */
    protected void handleKey(SelectionKey key) throws IOException {
        logger.trace("Key : " + key);
        if (!key.isValid()) {
            handleInvalidKey(key);
            return;
        }
        if (key.isConnectable()) {
            logger.trace("{} : Handling Connectable Key {}: {}...", this.id, key.hashCode(), key);
            handleConnect(key);
            return;
        }
        if (key.isReadable()) {
            logger.trace( "Handling Readable Key " + key + "...");
            handleRead(key);
            return;
        }
        if (key.isWritable()) {
            logger.trace( "Handling Writable Key " + key + "...");
            handleWrite(key);
            return;
        }
        if ((serverMode) && (key.isAcceptable())) {
            logger.trace("{} : Handling Acceptable Key {}: {}...", this.id, key.hashCode(), key);
            handleAccept(key);
        }
    }

    /**
     * It handles a CONNECT key, that is a confirmation that a connection to a remote Peer is successful.
     */
    protected void handleConnect(SelectionKey key) throws IOException {
        try {
            lock.writeLock().lock();
            KeyConnectionAttach keyConnection = (KeyConnectionAttach) key.attachment();

            logger.trace("{} : {} : [CONNECT Key] incoming Connection...", this.id, keyConnection.peerAddress);

            // Whatever happens, we remove this Peer from the "inProgress" Connections:
            if (key.attachment() != null) {
                inProgressConns.remove(keyConnection.peerAddress);
            }

            if (checkDuplicatedConnection(key, keyConnection.peerAddress)) {
                return;
            }

            // Check:
            // we accept the connection, unless this Peer is already register for disconnection, or the Handler
            // does not accept new connections anymore, or we've reached the Maximum Connections limit already:

            int limitNumConns = config.getMaxSocketConnections();
            if ((!keepConnecting) ||
                    (inProgressConns.size() + activeConns.size() >= limitNumConns)) {
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
     * It handles a READ key, that is some data is ready to read from one connection. Internally, the stream
     * representing this connection is used to read data from the socket (and return it to the "client" by using
     * the "onData" method)
     */
    protected void handleRead(SelectionKey key) throws IOException {
        // We read the data from the Peer (through the Stream wrapped out around it) and we run the callbacks:
        KeyConnectionAttach keyConnection = (KeyConnectionAttach) key.attachment();

        // If these bytes are the FIRST bytes coming from a Peer, we wait a bit JUST IN CASE, so we make sure that
        // all the events related to this Peer/Stream have been populated properly...
        // todo: is this really necessary?? This shows to me that there is a problem with the way we handle the startup
        if (!keyConnection.started) {
            logger.warn(">>>> DATA COMING FROM NOT-INITIALIZED STREAM: {}", keyConnection.peerAddress);
            try { Thread.sleep(50);} catch (InterruptedException ie) {}
            keyConnection.started = true;
        }

        int numBytesRead = ((NIOInputStream)keyConnection.stream.input()).readFromSocket();
        if (numBytesRead == -1) {
            logger.trace("{} : {} : Connection closed by the Remote Peer.", this.id, keyConnection.peerAddress);
            this.closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_REMOTE);
        }
    }

    /**
     * It handles a WRITE key, that is writing some data to the socket implementing that connection. Each connections
     * is representing by a ByteArrayStream, so we use it to write the data through the channel.
     */
    protected void handleWrite(SelectionKey key) throws IOException {
        // Write the data to the Peer (through the Stream wrapped out around it) and run the callbacks:
        KeyConnectionAttach keyConnection = (KeyConnectionAttach) key.attachment();
        // the checks below should not be necessary. They should be removed and the code allowed to create
        // exceptions. But if I do that right now then the majority of tests will fail.
        // There's something very messed up with the startup process that needs to be sorted out. todo
        if (keyConnection == null) {
            logger.warn(">>>> NULL ATTACHMENT: {}", key);
            return;
        }
        if (keyConnection.stream == null) {
            logger.warn(">>>> NULL STREAM: {}", keyConnection.peerAddress);
            return;
        }
        int numBytesWrite = ((NIOOutputStream) keyConnection.stream.output()).writeToSocket();
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

        try {
            lock.writeLock().lock();
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);
            Socket socket = channel.socket();
            PeerAddress peerAddress = new PeerAddress(socket.getInetAddress(), socket.getPort());

            logger.trace("{} : {} : [ACCEPT Key] incoming Connection...", this.id, socket.getRemoteSocketAddress());

            if (checkDuplicatedConnection(key, peerAddress)) {
                return;
            }

            // Check:
            // We accept the incoming conn only if the server is in "Accepting new connections" mode
            if (!keepConnecting) {
                logger.trace("{} : {} : discarding incoming connection (no more connections needed).", this.id, socket.getRemoteSocketAddress());
                closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
                return;
            }

            // Check:
            // We accept the incoming connection only if the Host is NOT Blacklisted
            // todo: we need to make sure this is implemented in the future
//            InetAddress peerIP = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress();
//            if (blacklist.contains(peerIP)) {
//                logger.trace("{} : {} : discarding incoming connection (blacklisted).", this.id, socket.getRemoteSocketAddress());
//                closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
//                return;
//            }

            // Check:
            // We haven't broken the "Maximum Socket connections" limit:

            if (activeConns.size() >= config.getMaxSocketConnections()) {
                logger.trace("{} : {} : no more connections allowed ({})", this.id, socket.getRemoteSocketAddress(), config.getMaxSocketConnections());
                closeKey(key, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
                return;
            }

            // IF we reach this far, we accept the incoming connection:

            logger.trace("{} : {} : accepting Connection...", this.id, socket.getRemoteSocketAddress());

            var selector = SelectorProvider.provider().openSelector();
            registerAndRunSelector(peerAddress, selector);
            SelectionKey clientKey = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            clientKey.attach(new KeyConnectionAttach(peerAddress));

            // We activate the Connection straight away:
            startPeerConnection(clientKey);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * It closes all Selection Keys
     */
    private void closeAllKeys(Selector selector) {
        logger.trace("{} : Closing all Keys", this.id);
        try {
            selector.wakeup();
            selector.keys().forEach(k -> closeKey(k, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL));
            selector.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.error("{} : Error closing Selector", this.id, ioe);
        }
    }


    // =============================================================================================================
    // LOGIC TO ADD MULTI-THREAD AT SELECTOR LEVEL
    // =============================================================================================================
    // Holds map between connected peer and selector
    private final Map<PeerAddress, Selector> selectorMap = new HashMap<>();
    // Holds map between selector and its running state. Setting it to false will shut down listening thread.
    private final Map<Selector, AtomicBoolean> isRunning = new HashMap<>();
    // ExecutionService used for handling peer selectors.
    private final ExecutorService executorService = ThreadUtils.getFixedThreadExecutorService("Peer-Connection", 50);

    private void registerAndRunSelector(PeerAddress peerAddress, Selector selector) {

        selectorMap.put(peerAddress, selector);
        var bool = new AtomicBoolean(true);
        isRunning.put(selector, bool);

        executorService.execute(() -> {
            try {
                while (bool.get()) {
                    handleSelectorKeys(selector);
                }
            } catch (InterruptedException | IOException e) {
                logger.error("Selector listener crashed!", e);
                throw new RuntimeException(e);
            } catch (ClosedSelectorException e) {
                // connection closed in between of execution
            } finally {
                isRunning.remove(selector);
                selectorMap.remove(peerAddress);
            }
        });
    }

    private void stopSelectorThreads() {
        Set<Selector> selectors = new HashSet<>(isRunning.keySet());
        selectors.forEach(selector ->
                stopSelectorThread(selector, peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL)
        );
        executorService.shutdown();
    }

    private void stopSelectorThread(Selector selector, PeerAddress peerAddress, PeerDisconnectedEvent.DisconnectedReason reason) {
        if (selector == this.mainSelector) {
            return;
        }
        notifyPeerDisconnection(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL);
        isRunning.get(selector).set(false);
        selector.wakeup();
    }
}