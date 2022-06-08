package io.bitcoinsv.jcl.net.network.config;


import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of the NetConfig Interface.
 */
public class NetworkConfigImpl extends HandlerConfig implements NetworkConfig {
    private int port;
    private OptionalInt maxSocketConnections;
    private OptionalInt maxSocketPendingConnections;
    private OptionalInt timeoutSocketConnection;
    private OptionalInt timeoutSocketRemoteConfirmation;
    private OptionalInt timeoutSocketIdle;
    private int maxSocketConnectionsOpeningAtSameTime;
    private int nioBufferSizeLowerBound;
    private int nioBufferSizeUpperBound;
    private int nioBufferSizeUpgrade;
    private int maxMessageSizeAvgInBytes;
    private boolean blockingOnListeners;

    public NetworkConfigImpl(int port,
                             OptionalInt maxSocketConnections,
                             OptionalInt maxSocketPendingConnections,
                             OptionalInt timeoutSocketConnection,
                             OptionalInt timeoutSocketRemoteConfirmation,
                             OptionalInt timeoutSocketIdle,
                             int maxSocketConnectionsOpeningAtSameTime,
                             int nioBufferSizeLowerBound,
                             int nioBufferSizeUpperBound,
                             int nioBufferSizeUpgrade,
                             int maxMessageSizeAvgInBytes,
                             boolean blockingOnListeners) {
        this.port = port;
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

    public int getPort()                                    { return this.port; }
    public OptionalInt getMaxSocketConnections()            { return this.maxSocketConnections; }
    public OptionalInt getMaxSocketPendingConnections()     { return this.maxSocketPendingConnections; }
    public OptionalInt getTimeoutSocketConnection()         { return this.timeoutSocketConnection; }
    public OptionalInt getTimeoutSocketRemoteConfirmation() { return this.timeoutSocketRemoteConfirmation;}
    public OptionalInt getTimeoutSocketIdle()               { return this.timeoutSocketIdle; }
    public int getMaxSocketConnectionsOpeningAtSameTime()   { return this.maxSocketConnectionsOpeningAtSameTime; }
    public int getNioBufferSizeLowerBound()                 { return this.nioBufferSizeLowerBound; }
    public int getNioBufferSizeUpperBound()                 { return this.nioBufferSizeUpperBound; }
    public int getNioBufferSizeUpgrade()                    { return this.nioBufferSizeUpgrade; }
    public int getMaxMessageSizeAvgInBytes()                { return this.maxMessageSizeAvgInBytes; }

    public NetworkConfigImplBuilder toBuilder() {
        return new NetworkConfigImplBuilder()
                .port(this.port)
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

    /**
     * Builder.
     */
    public static class NetworkConfigImplBuilder {
        private int port;
        private OptionalInt maxSocketConnections;
        private OptionalInt maxSocketPendingConnections;
        private OptionalInt timeoutSocketConnection;
        private OptionalInt timeoutSocketRemoteConfirmation;
        private OptionalInt timeoutSocketIdle;
        private int maxSocketConnectionsOpeningAtSameTime;
        private int nioBufferSizeLowerBound;
        private int nioBufferSizeUpperBound;
        private int nioBufferSizeUpgrade;
        private int maxMessageSizeAvgInBytes;
        private boolean blockingOnListeners;

        NetworkConfigImplBuilder() {}

        public NetworkConfigImpl.NetworkConfigImplBuilder port(int port) {
            this.port = port;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder maxSocketConnections(OptionalInt maxSocketConnections) {
            this.maxSocketConnections = maxSocketConnections;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder maxSocketPendingConnections(OptionalInt maxSocketPendingConnections) {
            this.maxSocketPendingConnections = maxSocketPendingConnections;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder timeoutSocketConnection(OptionalInt timeoutSocketConnection) {
            this.timeoutSocketConnection = timeoutSocketConnection;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder timeoutSocketRemoteConfirmation(OptionalInt timeoutSocketRemoteConfirmation) {
            this.timeoutSocketRemoteConfirmation = timeoutSocketRemoteConfirmation;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder timeoutSocketIdle(OptionalInt timeoutSocketIdle) {
            this.timeoutSocketIdle = timeoutSocketIdle;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder maxSocketConnectionsOpeningAtSameTime(int maxSocketConnectionsOpeningAtSameTime) {
            this.maxSocketConnectionsOpeningAtSameTime = maxSocketConnectionsOpeningAtSameTime;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder nioBufferSizeLowerBound(int nioBufferSizeLowerBound) {
            this.nioBufferSizeLowerBound = nioBufferSizeLowerBound;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder nioBufferSizeUpperBound(int nioBufferSizeUpperBound) {
            this.nioBufferSizeUpperBound = nioBufferSizeUpperBound;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder nioBufferSizeUpgrade(int nioBufferSizeUpgrade) {
            this.nioBufferSizeUpgrade = nioBufferSizeUpgrade;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder maxMessageSizeAvgInBytes(int maxMessageSizeAvgInBytes) {
            this.maxMessageSizeAvgInBytes = maxMessageSizeAvgInBytes;
            return this;
        }

        public NetworkConfigImpl.NetworkConfigImplBuilder blockingOnListeners(boolean blockingOnListeners) {
            this.blockingOnListeners = blockingOnListeners;
            return this;
        }

        public NetworkConfigImpl build() {
            return new NetworkConfigImpl(
                    port,
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