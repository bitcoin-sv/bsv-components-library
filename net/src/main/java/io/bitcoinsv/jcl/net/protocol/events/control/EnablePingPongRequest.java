/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PRequest;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-09 12:56
 *
 * A Request to Enable the Ping/Pong protocol for a particular Peer
 */
public final class EnablePingPongRequest extends P2PRequest {
    private final PeerAddress peerAddress;

    public EnablePingPongRequest(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public PeerAddress getPeerAddress() { return this.peerAddress; }

    @Override
    public String toString() {
        return "EnablePingPongRequest(peerAddress=" + this.getPeerAddress() + ")";
    }
}
