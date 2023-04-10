package io.bitcoinsv.bsvcl.net.network.streams;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A PeerStream is an abstraction that allows us to send data/Events to a Peer.
 */
public interface PeerStream<T> {

    /** Returns the InputStream that allows us to react to data/events received */
    PeerInputStream<T> input();

    /** Returns the OutputStream that allows us to send data/events */
    PeerOutputStream<T> output();

    /** both Input or Output Streams are meant to be connected to the same Peer, so we return any of them */
    default PeerAddress getPeerAddress() {
        if (input() != null) return input().getPeerAddress();
        else if (output() != null) return output().getPeerAddress();
        else return null;
    }
    /** returns the State of the Stream. Implementtion might overwrite this */
    default StreamState getState() { return null; }
}