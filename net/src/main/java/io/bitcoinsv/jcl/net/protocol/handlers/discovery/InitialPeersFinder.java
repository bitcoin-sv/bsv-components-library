/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.handlers.discovery;

import io.bitcoinsv.jcl.net.network.PeerAddress;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * When the Network activity Start, the Discovery ProtocolHandler needs an initial list of Peers to
 * connect to. After the connection is established with some Peers, we can rely on the Node-Discovery
 * P2P to discover new Peers in the Network, but we need that first list to start the first time.
 *
 * Implementations of this interface will be responsible fro retrieving that initial List of Peers to
 * connect to on Starting the Network activity
 */
public interface InitialPeersFinder {
    /**
     * Returns an initial List of Peers to connect to right after starting the Network Activity
     */
    List<PeerAddress> findPeers();
}
