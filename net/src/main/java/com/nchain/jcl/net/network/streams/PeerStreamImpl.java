package com.nchain.jcl.net.network.streams;



import com.nchain.jcl.base.tools.streams.StreamImpl;
import com.nchain.jcl.net.network.PeerAddress;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 16:35
 *
 * A base implementation of a PeerStream. A PeerStream is very similar to a regular Stream, but with
 * these differences:
 *  - A PeerStream includes an additional method "getPeerAddress()" to return the Peer this Stream is
 *    ultimately connected to.
 *  - A PeerStream includes a method to return the "State" of the Stream ("getState()".
 *  - the "input" and "output" of a PeerStream also have the differences mentioned above, os instead of
 *    "InputStream and "OutputStream" classes, we have now "PeerInputStream" and "PeerOutputStream". These
 *    classes are similar to the "regular" InputStream and outputStream, but also including "getPeerAddress()"
 *    and "getState()".
 */
public abstract class PeerStreamImpl<S,T> extends StreamImpl<S,T> implements PeerStream<S> {

    public PeerStreamImpl(ExecutorService executor, PeerStream<T> streamOrigin) {
        super(executor, streamOrigin);
    }

    public abstract PeerInputStream<S> buildInputStream();
    public abstract PeerOutputStream<S> buildOutputStream();

    @Override
    public PeerInputStream<S> input() {
        return (PeerInputStream) inputStream;
    }
    @Override
    public PeerOutputStream<S> output() {
        return (PeerOutputStream) outputStream;
    }
    @Override
    public PeerAddress getPeerAddress() {
        return ((PeerStream) streamOrigin).getPeerAddress();
    }
}
