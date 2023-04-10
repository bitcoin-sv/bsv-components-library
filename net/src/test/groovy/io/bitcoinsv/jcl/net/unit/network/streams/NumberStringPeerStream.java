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
public class NumberStringPeerStream extends PeerStreamImpl<Integer, String> {

    public NumberStringPeerStream(ExecutorService executor, PeerStream<String> streamOrigin) {
        super(executor, streamOrigin);
    }

    @Override
    public PeerInputStream<Integer> buildInputStream() {
        return new NumberStringInputStream(peerAddress, super.executor, super.streamOrigin.input());
    }
    @Override
    public PeerOutputStream<Integer> buildOutputStream() {
        return new NumberStringOutputStream(peerAddress, super.streamOrigin.output());
    }
}
