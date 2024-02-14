package io.bitcoinsv.jcl.net.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This event represent an error thrown by an Stream, which most probably has been thrown during
 * the transformation function.
 */
public class StreamErrorEvent extends StreamEvent {
    private final PeerAddress peerAddress; //might help if we need to identify the peer that caused the error
    private final Throwable exception;

    public StreamErrorEvent(PeerAddress peerAddress, Throwable exception) {
        this.peerAddress = peerAddress;
        this.exception = exception;
    }

    public StreamErrorEvent(Throwable exception)    { this(null, exception); }
    public PeerAddress getPeerAddress()             { return this.peerAddress; }
    public Throwable getException()                 { return this.exception; }
}
