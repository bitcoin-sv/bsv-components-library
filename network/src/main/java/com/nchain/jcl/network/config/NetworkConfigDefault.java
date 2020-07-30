package com.nchain.jcl.network.config;

import lombok.Getter;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-05 16:22
 *
 * A Simple NetWork configuration that is supposed to work fine in a local Development
 * Environment, like a regular laptop with at least 8GB of RAM.
 */
@Getter
public class NetworkConfigDefault extends NetworkConfigImpl {

    private static final int port = 8333;
    private static final OptionalInt maxSocketConnections = OptionalInt.of(1000);
    private static final OptionalInt maxSocketPendingConnections = OptionalInt.of(5000);
    private static final OptionalInt timeoutSocketConnection = OptionalInt.of(2000);
    private static final OptionalInt timeoutSocketIdle = OptionalInt.empty();
    private static final int nioBufferSizeLowerBound = 4096;
    private static final int nioBufferSizeUpperBound = 65536;
    private static final int nioBufferSizeUpgrade = 10_000_000;
    private static final int maxMessageSizeAvgInBytes = 1000; // TODO :CAREFUL
    private static final boolean blockingOnListeners = false;

    /** Constructor */
    public NetworkConfigDefault() {
        super(
                port,
                maxSocketConnections,
                maxSocketPendingConnections,
                timeoutSocketConnection,
                timeoutSocketIdle,
                nioBufferSizeLowerBound,
                nioBufferSizeUpperBound,
                nioBufferSizeUpgrade,
                maxMessageSizeAvgInBytes,
                blockingOnListeners);
    }
}