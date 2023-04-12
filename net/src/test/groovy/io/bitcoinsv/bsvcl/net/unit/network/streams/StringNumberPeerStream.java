package io.bitcoinsv.bsvcl.net.unit.network.streams;

import io.bitcoinsv.bsvcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerStreamImpl;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-02-11
 */
public class StringNumberPeerStream extends PeerStreamImpl<String, Integer> {

    public StringNumberPeerStream(PeerStream<Integer> streamOrigin) {
        super(streamOrigin);
    }

    @Override
    public PeerInputStream<String> buildInputStream() {
        return new StringNumberInputStream(peerAddress, super.streamOrigin.input());
    }
    @Override
    public PeerOutputStream<String> buildOutputStream() {
        return new StringNumberOutputStream(peerAddress, super.streamOrigin.output());
    }
}