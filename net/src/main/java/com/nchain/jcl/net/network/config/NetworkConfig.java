package com.nchain.jcl.net.network.config;

import java.util.OptionalInt;

/**
 *
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration needed by the Net package. Implementations will provide specific values
 */
public interface NetworkConfig {

    /** Listening port for Peers in the Blockchain Network */
    int getPort();

    /** Maximum Socket connections that can be open simultaneously open to the network */
    OptionalInt getMaxSocketConnections();

    /** Maximum connection waiting to be opened */
    OptionalInt getMaxSocketPendingConnections();

    /** Maximum number of millisecs that it can tacke a Socket to connect */
    OptionalInt getTimeoutSocketConnection();

    /** Maximum number of millisecs to wait for an idle Socket before the connection is closed */
    OptionalInt getTimeoutSocketIdle();

    /** Only relevant for NIO-based implementations. Lower bound for the Socket Buffer size */
    int getNioBufferSizeLowerBound();

    /** Only relevant for NIO-based implementations. Upper bound for the Socket Buffer size */
    int getNioBufferSizeUpperBound();

    /** Only relevant for NIO-based implementations. Buffer size in case we a re downloading big amounts of data */
    int getNioBufferSizeUpgrade();

    /** An aproximation of the max size of a message that will be sent/receive by the Net */
    int getMaxMessageSizeAvgInBytes();


    /**
     * indicates if the listeners are running in blocking mode (same Thread as the system)
     * If they are running in blocking mode, they might block the system, otherwise the general
     * performance of the Network Component shouldn't be affected.
     * */
    boolean isBlockingOnListeners();

}