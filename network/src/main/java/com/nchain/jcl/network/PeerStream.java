package com.nchain.jcl.network;

import com.nchain.jcl.tools.streams.StreamState;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-11 09:55
 *
 * This interface is an extension to be added to a general Stream (InputStream, OutputStream,
 * InputStreamSource, OutputStreamDestination, Stream or StreamEndpoint).
 * If a Stream also implements this interfaces, thn it means that the Stream is part of a chain
 * of Streams that is connected to a Peer, either as a Source, Destination or both.
 */
public interface PeerStream {
    /** Returns the Peer the Stream its connected to */
    PeerAddress getPeerAddress();

    StreamState getState();
}
