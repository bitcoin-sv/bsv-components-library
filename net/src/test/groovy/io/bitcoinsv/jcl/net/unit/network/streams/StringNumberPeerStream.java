/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.network.streams;

import io.bitcoinsv.jcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerStream;
import io.bitcoinsv.jcl.net.network.streams.PeerStreamImpl;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-02-11
 */
public class StringNumberPeerStream extends PeerStreamImpl<String, Integer> {

    public StringNumberPeerStream(ExecutorService executor, PeerStream<Integer> streamOrigin) {
        super(executor, streamOrigin);
    }

    @Override
    public PeerInputStream<String> buildInputStream() {
        return new StringNumberInputStream(peerAddress, super.executor, super.streamOrigin.input());
    }
    @Override
    public PeerOutputStream<String> buildOutputStream() {
        return new StringNumberOutputStream(peerAddress, super.executor, super.streamOrigin.output());
    }
}
