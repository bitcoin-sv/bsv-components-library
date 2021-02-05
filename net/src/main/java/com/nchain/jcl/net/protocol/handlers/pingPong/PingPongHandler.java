package com.nchain.jcl.net.protocol.handlers.pingPong;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
