package io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * Event triggered when an Error has been triggered during a Large Message Deserialization
 */
public class MsgPartDeserializationErrorEvent extends Event {
    private final PeerAddress peerAddress; //might help if we need to identify the peer that caused the error
    private final Exception exception;

    public MsgPartDeserializationErrorEvent(PeerAddress peerAddress, Exception exception) {
        this.peerAddress = peerAddress;
        this.exception = exception;
    }

    public MsgPartDeserializationErrorEvent(Exception exception) {
        this(null, exception);
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    public Exception getException() {
        return this.exception;
    }
}
