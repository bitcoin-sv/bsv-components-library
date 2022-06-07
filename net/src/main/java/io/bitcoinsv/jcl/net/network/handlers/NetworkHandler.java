package io.bitcoinsv.jcl.net.network.handlers;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.DisconnectPeerRequest;
import io.bitcoinsv.jcl.net.network.events.PeerDisconnectedEvent;
import io.bitcoinsv.jcl.tools.handlers.Handler;

import java.util.List;
import java.util.stream.Collectors;

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

    void processDisconnectRequest(DisconnectPeerRequest request);
    void processDisconnectRequests(List<DisconnectPeerRequest> requests);

    default void disconnect(PeerAddress peerAddress) {
        processDisconnectRequest(new DisconnectPeerRequest(peerAddress, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL));
    }

    default void disconnect(List<PeerAddress> peerAddressList) {
        processDisconnectRequests(peerAddressList
                .stream()
                .map(p -> new DisconnectPeerRequest(p, PeerDisconnectedEvent.DisconnectedReason.DISCONNECTED_BY_LOCAL))
                .collect(Collectors.toList()));
    }

    void disconnectAllExcept(List<PeerAddress> peerAddresses);
}
