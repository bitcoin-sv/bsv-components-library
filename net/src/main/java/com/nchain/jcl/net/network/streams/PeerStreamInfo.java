package com.nchain.jcl.net.network.streams;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.streams.StreamState;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This interface is an extension to be added to a general Stream (InputStream, OutputStream,
 * InputStreamSource, OutputStreamDestination, Stream or StreamEndpoint).
 * If a Stream also implements this interfaces, thn it means that the Stream is part of a chain
 * of Streams that is connected to a Peer, either as a Source, Destination or both.
 */
public interface PeerStreamInfo {
    /** Returns the Peer the Stream its connected to */
    PeerAddress getPeerAddress();

    StreamState getState();
}
