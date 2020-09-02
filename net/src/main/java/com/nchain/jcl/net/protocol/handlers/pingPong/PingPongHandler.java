package com.nchain.jcl.net.protocol.handlers.pingPong;

import com.nchain.jcl.base.tools.handlers.Handler;
import com.nchain.jcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:49
 *
 * Operations provided by the Ping/Pong P2P Handler
 */
public interface PingPongHandler extends Handler {

    String HANDLER_ID = "PingPong-Handler";

    @Override
    default String getId() { return HANDLER_ID; }

    // Enable/Disable the Ping/Pong Verifications for a specific Peer
    void disablePingPong(PeerAddress peerAddress);
    void enablePingPong(PeerAddress peerAddress);
}
