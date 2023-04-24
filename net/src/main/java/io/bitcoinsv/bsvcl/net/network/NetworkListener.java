package io.bitcoinsv.bsvcl.net.network;

import io.bitcoinsv.bsvcl.common.ServiceState;
import io.bitcoinsv.bsvcl.net.P2P;
import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

/** The NetworkListener listens for and accepts incoming connections. It passes the inbound
 * connection to the P2P for assignment to a NetworkController. */
public class NetworkListener extends Thread {
    private final LoggerUtil logger;
    private final P2P p2p;
    private PeerAddress listenAddress;
    private ServiceState state = ServiceState.STOPPED;
    private Selector mainSelector = null;
    // latch that is released when the server has been initialized
    private final CountDownLatch initializedLatch = new CountDownLatch(1);

    public NetworkListener(String id, P2P p2p, PeerAddress listenAddress) {
        this.p2p = p2p;
        this.listenAddress = listenAddress;
        this.logger = new LoggerUtil(id, NetworkListener.class);
    }

    public void initiateShutdown() {
        state = ServiceState.STOPPING;
        try {
            mainSelector.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        state = ServiceState.STARTING;
        try {
            this.mainSelector = Selector.open();
            SocketAddress serverSocketAddress = new InetSocketAddress(listenAddress.getIp(), listenAddress.getPort());
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);
            serverSocket.socket().setReuseAddress(true);
            serverSocket.socket().bind(serverSocketAddress);
            serverSocket.register(mainSelector, SelectionKey.OP_ACCEPT);

            // In case the local getPort is ZERO, that means that the system will pick one up for us, so we need to
            // update it after it's been assigned:
            if (listenAddress.getPort() == 0)
                listenAddress = new PeerAddress(listenAddress.getIp(), serverSocket.socket().getLocalPort());
            state = ServiceState.RUNNING;
            initializedLatch.countDown();
            logger.info("NetworkListener started on", listenAddress);

            while (state != ServiceState.STOPPING) {
                mainSelector.select();
                for (SelectionKey key : mainSelector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        if (state == ServiceState.STOPPING || state == ServiceState.PAUSED) {
                            key.cancel();
                            continue;
                        }
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel channel = serverChannel.accept();
                        channel.configureBlocking(false);
                        Socket socket = channel.socket();
                        PeerAddress peerAddress = new PeerAddress(socket.getInetAddress(), socket.getPort());
                        p2p.acceptConnection(peerAddress, channel);
                    }
                }
                mainSelector.selectedKeys().clear();
            }

            state = ServiceState.STOPPED;
        } catch (IOException e) {
            state = ServiceState.ERROR;
            throw new RuntimeException(e);
        }
    }

    public void pauseService() {
        if (state == ServiceState.RUNNING) {
            state = ServiceState.PAUSED;
        }
    }

    public void resumeService() {
        if (state == ServiceState.PAUSED) {
            state = ServiceState.RUNNING;
        }
    }

    public PeerAddress getListenAddress() {
        return listenAddress;
    }

    public ServiceState getServiceState() {
        return state;
    }

    public void awaitInitialization() throws InterruptedException {
        initializedLatch.await();
    }
}
