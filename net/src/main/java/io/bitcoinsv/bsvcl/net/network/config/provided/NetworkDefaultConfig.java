package io.bitcoinsv.bsvcl.net.network.config.provided;

import io.bitcoinsv.bsvcl.net.network.config.NetworkConfigImpl;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Simple NetWork configuration that is supposed to work fine in a local Development
 * Environment, like a regular laptop with at least 8GB of RAM.
 */
public class NetworkDefaultConfig extends NetworkConfigImpl {

    private static final int port = 8333;
    private static final OptionalInt maxSocketConnections = OptionalInt.of(1000);
    private static final OptionalInt maxSocketPendingConnections = OptionalInt.of(5000);
    private static final OptionalInt timeoutSocketConnection = OptionalInt.of(500);
    private static final OptionalInt timeoutSocketRemoteConfirmation = OptionalInt.of(500);
    private static final OptionalInt timeoutSocketIdle = OptionalInt.empty();
    private static final int maxSocketConnectionsOpeningAtSameTime = 50;
    private static final int nioBufferSizeLowerBound = 4096;
    private static final int nioBufferSizeUpperBound = 65536;
    private static final int nioBufferSizeUpgrade = 10_000_000;
    private static final int maxMessageSizeAvgInBytes = 1000; // TODO :CAREFUL
    private static final boolean blockingOnListeners = false;

    /** Constructor */
    public NetworkDefaultConfig() {
        super(
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