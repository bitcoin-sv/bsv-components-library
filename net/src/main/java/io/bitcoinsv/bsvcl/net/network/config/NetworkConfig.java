package io.bitcoinsv.bsvcl.net.network.config;

import java.util.OptionalInt;

/**
 *
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration needed by the Net package. Implementations will provide specific values
 */
public interface NetworkConfig {

    /** Default port for peers in the Blockchain Network */
    int getDefaultPort();

    /** Our listening port */
    int getListeningPort();

    /** Maximum Socket connections that can be open simultaneously open to the network */
    OptionalInt getMaxSocketConnections();

    /** Maximum connection waiting to be opened */
    OptionalInt getMaxSocketPendingConnections();

    /** Maximum number of millisecs that it can wait for a Socket to connect to a Remote port*/
    OptionalInt getTimeoutSocketConnection();

    /** Maximum number of millisecs that it can wait for a Remote Socket to confirm the connection */
    OptionalInt getTimeoutSocketRemoteConfirmation();

    /** Maximum number of millisecs to wait for an idle Socket before the connection is closed */
    OptionalInt getTimeoutSocketIdle();

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
    int getMaxSocketConnectionsOpeningAtSameTime();

    /** Only relevant for NIO-based implementations. Lower bound for the Socket Buffer size */
    int getNioBufferSizeLowerBound();

    /** Only relevant for NIO-based implementations. Upper bound for the Socket Buffer size */
    int getNioBufferSizeUpperBound();

    /** Only relevant for NIO-based implementations. Buffer size in case we a re downloading big amounts of data */
    int getNioBufferSizeUpgrade();

    /** An aproximation of the max size of a message that will be sent/receive by the Net */
    int getMaxMessageSizeAvgInBytes();

}