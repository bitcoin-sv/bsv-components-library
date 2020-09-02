package com.nchain.jcl.net.network.config;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.network.config.NetworkConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.OptionalInt;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-11
 *
 * An implementation of the NetConfig Interface.
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class NetworkConfigImpl extends HandlerConfig implements NetworkConfig {
    private int port;
    private OptionalInt maxSocketConnections;
    private OptionalInt maxSocketPendingConnections;
    private OptionalInt timeoutSocketConnection;
    private OptionalInt timeoutSocketIdle;
    private int nioBufferSizeLowerBound;
    private int nioBufferSizeUpperBound;
    private int nioBufferSizeUpgrade;
    private int maxMessageSizeAvgInBytes;
    private boolean blockingOnListeners;

}