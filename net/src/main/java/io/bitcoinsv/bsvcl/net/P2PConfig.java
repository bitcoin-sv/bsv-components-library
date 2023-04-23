package io.bitcoinsv.bsvcl.net;

//  @author i.fernandez@nchain.com
//  Copyright (c) 2018-2023 nChain Ltd

import io.bitcoinsv.bsvcl.common.handlers.HandlerConfig;

import java.util.OptionalInt;

/**
 * Configurations needed by the P2P package.
 */
public class P2PConfig {
    private final int defaultPort;
    private final int listeningPort;
    private final int maxSocketConnections;
    private final int maxSocketPendingConnections;
    private final int timeoutSocketConnection;
    private final int timeoutSocketRemoteConfirmation;
    private final int timeoutSocketIdle;
    private final int maxSocketConnectionsOpeningAtSameTime;
    private final int nioBufferSizeLowerBound;
    private final int nioBufferSizeUpperBound;
    private final int nioBufferSizeUpgrade;
    private final int maxMessageSizeAvgInBytes;
    private final boolean blockingOnListeners;

    public P2PConfig(int defaultPort, int listeningPort,
                     int maxSocketConnections,
                     int maxSocketPendingConnections,
                     int timeoutSocketConnection,
                     int timeoutSocketRemoteConfirmation,
                     int timeoutSocketIdle,
                     int maxSocketConnectionsOpeningAtSameTime,
                     int nioBufferSizeLowerBound,
                     int nioBufferSizeUpperBound,
                     int nioBufferSizeUpgrade,
                     int maxMessageSizeAvgInBytes,
                     boolean blockingOnListeners) {
        this.defaultPort = defaultPort;
        this.listeningPort = listeningPort;
        this.maxSocketConnections = maxSocketConnections;
        this.maxSocketPendingConnections = maxSocketPendingConnections;
        this.timeoutSocketConnection = timeoutSocketConnection;
        this.timeoutSocketRemoteConfirmation = timeoutSocketRemoteConfirmation;
        this.timeoutSocketIdle = timeoutSocketIdle;
        this.maxSocketConnectionsOpeningAtSameTime = maxSocketConnectionsOpeningAtSameTime;
        this.nioBufferSizeLowerBound = nioBufferSizeLowerBound;
        this.nioBufferSizeUpperBound = nioBufferSizeUpperBound;
        this.nioBufferSizeUpgrade = nioBufferSizeUpgrade;
        this.maxMessageSizeAvgInBytes = maxMessageSizeAvgInBytes;
        this.blockingOnListeners = blockingOnListeners;
    }

    public static NetworkConfigImplBuilder builder()        { return new NetworkConfigImplBuilder(); }

    /** Default port for peers in the Blockchain Network */
    public int getDefaultPort()                             { return this.defaultPort; }

    /** Our listening port */
    public int getListeningPort()                           { return this.listeningPort; }

    /** Maximum number of socket connections that can be open simultaneously */
    public int getMaxSocketConnections()            { return this.maxSocketConnections; }

    /** Maximum number of connections that can wait to be opened */
    public int getMaxSocketPendingConnections()     { return this.maxSocketPendingConnections; }

    /** Maximum number of millisecs to wait for a socket to connect to a remote port*/
    public int getTimeoutSocketConnection()         { return this.timeoutSocketConnection; }

    /** Maximum number of millisecs to wait for a remote socket to confirm the connection */
    public int getTimeoutSocketRemoteConfirmation() { return this.timeoutSocketRemoteConfirmation;}

    /** Maximum number of millisecs to wait for an idle socket before the connection is closed */
    public int getTimeoutSocketIdle()               { return this.timeoutSocketIdle; }


    /**
     * This number indicates the number of connection that the Network Handler will try to open, every time it runs low
     * on connections and needs more. Opening a new Connection might be expensive in terms of Messages exchange and
     * threads, so use this field with precaution:
     *  - If the expected traffic is VERY HIGH, keep this number low (0-10). Sicne the traffic is VERY HIGH, we might
     *    proabaly have a big number of Threads running in the Bus, we do NOT want even more due to new comnnections
     *    being opened at the same time.
     * - If the expected traffic is LOW, you can set a number about (100-200), this will make the Network Handler to
     *   open 100-300 connections to new Peres simultaneously. That is exepnsice in therms of Threads, but since the
     *   traffic is low we are not expecting any issue. Use this option is you want to connect to new Peers FAST.
     * @return
     */
    public int getMaxSocketConnectionsOpeningAtSameTime()   { return this.maxSocketConnectionsOpeningAtSameTime; }

    /** Lower bound for the Socket Buffer size */
    public int getNioBufferSizeLowerBound()                 { return this.nioBufferSizeLowerBound; }
    /** Upper bound for the Socket Buffer size */
    public int getNioBufferSizeUpperBound()                 { return this.nioBufferSizeUpperBound; }

    /** Only relevant for NIO-based implementations. Buffer size in case we are downloading large amounts of data */
    public int getNioBufferSizeUpgrade()                    { return this.nioBufferSizeUpgrade; }

    /** An approximation of the max size of a message that will be sent/receive over the network */
    public int getMaxMessageSizeAvgInBytes()                { return this.maxMessageSizeAvgInBytes; }

    public NetworkConfigImplBuilder toBuilder() {
        return new NetworkConfigImplBuilder()
                .defaultPort(this.defaultPort)
                .listeningPort(this.listeningPort)
                .maxSocketConnections(this.maxSocketConnections)
                .maxSocketPendingConnections(this.maxSocketPendingConnections)
                .maxSocketConnectionsOpeningAtSameTime(this.maxSocketConnectionsOpeningAtSameTime)
                .timeoutSocketConnection(this.timeoutSocketConnection)
                .timeoutSocketRemoteConfirmation(this.timeoutSocketRemoteConfirmation)
                .timeoutSocketIdle(this.timeoutSocketIdle)
                .nioBufferSizeLowerBound(this.nioBufferSizeLowerBound)
                .nioBufferSizeUpperBound(this.nioBufferSizeUpperBound)
                .nioBufferSizeUpgrade(this.nioBufferSizeUpgrade)
                .maxMessageSizeAvgInBytes(this.maxMessageSizeAvgInBytes)
                .blockingOnListeners(this.blockingOnListeners);
    }

    public static class NetworkConfigImplBuilder {
        private int defaultPort = 8333;
        private int listeningPort = 8333;
        private int maxSocketConnections = 1000;
        private int maxSocketPendingConnections = 5000;
        private int timeoutSocketConnection = 500;
        private int timeoutSocketRemoteConfirmation = 500;
        private int timeoutSocketIdle = 5*60*1000;  // 5 minutes
        private int maxSocketConnectionsOpeningAtSameTime = 50;
        private int nioBufferSizeLowerBound = 4096;
        private int nioBufferSizeUpperBound = 65536;
        private int nioBufferSizeUpgrade = 10_000_000;
        private int maxMessageSizeAvgInBytes = 1000;
        private boolean blockingOnListeners = false;

        NetworkConfigImplBuilder() {}

        public P2PConfig.NetworkConfigImplBuilder defaultPort(int defaultPort) {
            this.defaultPort = defaultPort;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder listeningPort(int listeningPort) {
            this.listeningPort = listeningPort;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder maxSocketConnections(int maxSocketConnections) {
            this.maxSocketConnections = maxSocketConnections;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder maxSocketPendingConnections(int maxSocketPendingConnections) {
            this.maxSocketPendingConnections = maxSocketPendingConnections;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder timeoutSocketConnection(int timeoutSocketConnection) {
            this.timeoutSocketConnection = timeoutSocketConnection;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder timeoutSocketRemoteConfirmation(int timeoutSocketRemoteConfirmation) {
            this.timeoutSocketRemoteConfirmation = timeoutSocketRemoteConfirmation;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder timeoutSocketIdle(int timeoutSocketIdle) {
            this.timeoutSocketIdle = timeoutSocketIdle;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder maxSocketConnectionsOpeningAtSameTime(int maxSocketConnectionsOpeningAtSameTime) {
            this.maxSocketConnectionsOpeningAtSameTime = maxSocketConnectionsOpeningAtSameTime;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder nioBufferSizeLowerBound(int nioBufferSizeLowerBound) {
            this.nioBufferSizeLowerBound = nioBufferSizeLowerBound;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder nioBufferSizeUpperBound(int nioBufferSizeUpperBound) {
            this.nioBufferSizeUpperBound = nioBufferSizeUpperBound;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder nioBufferSizeUpgrade(int nioBufferSizeUpgrade) {
            this.nioBufferSizeUpgrade = nioBufferSizeUpgrade;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder maxMessageSizeAvgInBytes(int maxMessageSizeAvgInBytes) {
            this.maxMessageSizeAvgInBytes = maxMessageSizeAvgInBytes;
            return this;
        }

        public P2PConfig.NetworkConfigImplBuilder blockingOnListeners(boolean blockingOnListeners) {
            this.blockingOnListeners = blockingOnListeners;
            return this;
        }

        public P2PConfig build() {
            return new P2PConfig(
                    defaultPort,
                    listeningPort,
                    maxSocketConnections,
                    maxSocketPendingConnections,
                    timeoutSocketConnection,
                    timeoutSocketRemoteConfirmation,
                    timeoutSocketIdle,
                    maxSocketConnectionsOpeningAtSameTime,
                    nioBufferSizeLowerBound,
                    nioBufferSizeUpperBound,
                    nioBufferSizeUpgrade,
                    maxMessageSizeAvgInBytes,
                    blockingOnListeners);
        }
    }
}