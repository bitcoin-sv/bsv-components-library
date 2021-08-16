package io.bitcoinsv.jcl.net.protocol.events.control;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.events.P2PEvent;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Event triggered when a Peer has failed to perform the Ping/Pong P2P
 */
public final class PingPongFailedEvent extends P2PEvent {

    /** Stores the different reason why a Ping-Pong might fail */
    public enum PingPongFailedReason {
        MISSING_PING,
        TIMEOUT,
        WRONG_NONCE
    }

    private final PeerAddress peerAddress;
    private final PingPongFailedReason reason;

    public PingPongFailedEvent(PeerAddress peerAddress, PingPongFailedReason reason) {
        this.peerAddress = peerAddress;
        this.reason = reason;
    }

    public PeerAddress getPeerAddress()     { return this.peerAddress; }
    public PingPongFailedReason getReason() { return this.reason; }

    @Override
    public String toString() {
        return "PingPongFailedEvent(peerAddress=" + this.getPeerAddress() + ", reason=" + this.getReason() + ")";
    }
}
