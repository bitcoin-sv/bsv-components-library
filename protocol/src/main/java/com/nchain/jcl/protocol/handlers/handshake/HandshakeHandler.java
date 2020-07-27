package com.nchain.jcl.protocol.handlers.handshake;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.tools.handlers.Handler;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-06 12:57
 *
 * Operations provided by the Handshake Handler
 */
public interface HandshakeHandler extends Handler {
    String HANDLER_ID = "Handshake-Handler";

    @Override
    default String getId() { return HANDLER_ID; }

    /** Returns TRUE if the Peer is currently Handshaked */
    boolean isHandshaked(PeerAddress peerAddress);
}
