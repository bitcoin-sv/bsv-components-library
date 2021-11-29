package com.nchain.jcl.net.network.handlers;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.handlers.Handler;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations of a Network Handler. Takes care of stablishing physical connections to other remote
 * Peers in the Network.
 *
 *
 */
public interface NetworkHandler extends Handler {

    String HANDLER_ID = "Network";

    @Override
    default String getId() { return HANDLER_ID; }

    @Override
    NetworkHandlerState getState();

    // Methods to start/Stop the Network Activity and other basic info:

    void start();
    void startServer();
    void stop();
    PeerAddress getPeerAddress();

    // By default, the NetworkHandler will ALWAYS connect to as many Peers as possible, until the "StopConnecting()"
    // method is invoked. And the connecting will resume when the "resumeConnecting()" method is invoked. So the
    // control over when to start/stop connecting is taken care by other higher-layers of the application.

    void stopConnecting();
    void resumeConnecting();

    // Methods to connect/disconnect/interact with Peers:

    void connect(PeerAddress peerAddress);
    void connect(List<PeerAddress> peerAddressList);
    void disconnect(PeerAddress peerAddress);
    void disconnect(List<PeerAddress> peerAddressList);
    void disconnectAllExcept(List<PeerAddress> peerAddresses);
}
