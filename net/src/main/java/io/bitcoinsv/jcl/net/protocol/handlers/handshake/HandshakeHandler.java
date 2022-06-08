package io.bitcoinsv.jcl.net.protocol.handlers.handshake;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations provided by the Handshake Handler
 */
public interface HandshakeHandler extends Handler {
    String HANDLER_ID = "Handshake";

    @Override
    default String getId() { return HANDLER_ID; }

    /** Returns TRUE if the Peer is currently Handshaked */
    boolean isHandshaked(PeerAddress peerAddress);
}
