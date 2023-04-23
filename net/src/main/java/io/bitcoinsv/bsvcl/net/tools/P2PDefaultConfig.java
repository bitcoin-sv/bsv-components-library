package io.bitcoinsv.bsvcl.net.tools;

import io.bitcoinsv.bsvcl.net.P2PConfig;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Simple NetWork configuration that is supposed to work fine in a local Development
 * Environment, like a regular laptop with at least 8GB of RAM.
 */
public class P2PDefaultConfig extends P2PConfig {

    private static final int defaultPort = 8333;
    private static final int listeningPort = 8333;
    private static final int maxSocketConnections = 1000;
    private static final int maxSocketPendingConnections = 5000;
    private static final int timeoutSocketConnection = 500;
    private static final int timeoutSocketRemoteConfirmation = 500;
    private static final int timeoutSocketIdle = 5*60*1000; // 5 minutes
    private static final int maxSocketConnectionsOpeningAtSameTime = 50;
    private static final int nioBufferSizeLowerBound = 4096;
    private static final int nioBufferSizeUpperBound = 65536;
    private static final int nioBufferSizeUpgrade = 10_000_000;
    private static final int maxMessageSizeAvgInBytes = 1000; // TODO :CAREFUL
    private static final boolean blockingOnListeners = false;

    /** Constructor */
    public P2PDefaultConfig() {
        super(
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