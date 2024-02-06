package io.bitcoinsv.jcl.net.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;

/**
 * This is a general event intended for invalid messages (not corrupted messages and/or data streams!).
 * Preservers peer information and reason (why the message is invalid).
 */
public class InvalidMessageErrorEvent extends StreamEvent {
    private final PeerAddress peerAddress;
    private final String reason;

    public InvalidMessageErrorEvent(PeerAddress peerAddress, String reason) {
        this.peerAddress = peerAddress;
        this.reason = reason;
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    public String getReason() {
        return this.reason;
    }
}
