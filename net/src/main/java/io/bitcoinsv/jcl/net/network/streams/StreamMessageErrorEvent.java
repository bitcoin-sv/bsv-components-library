package io.bitcoinsv.jcl.net.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;

/**
 * This is a general event intended for various errors during message deserialization.
 * Preservers peer information, (error) message and cause.
 */
public class StreamMessageErrorEvent extends StreamEvent {
    private final PeerAddress peerAddress;
    private final String message;
    private final Throwable cause;

    public StreamMessageErrorEvent(PeerAddress peerAddress, String message, Throwable cause) {
        this.peerAddress = peerAddress;
        this.message = message;
        this.cause = cause;
    }

    public StreamMessageErrorEvent(PeerAddress peerAddress, Throwable cause) {
        this(peerAddress, cause.getMessage(), cause);
    }

    public StreamMessageErrorEvent(PeerAddress peerAddress, String message) {
        this(peerAddress, message, null);
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    public Throwable getCause() {
        return this.cause;
    }

    public String getMessage() {
        return this.message;
    }
}
