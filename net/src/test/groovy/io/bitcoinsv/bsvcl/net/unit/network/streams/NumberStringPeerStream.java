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
public class NumberStringPeerStream extends PeerStreamImpl<Integer, String> {

    public NumberStringPeerStream(PeerStream<String> streamOrigin) {
        super( streamOrigin);
    }

    @Override
    public PeerInputStream<Integer> buildInputStream() {
        return new NumberStringInputStream(peerAddress, super.streamOrigin.input());
    }
    @Override
    public PeerOutputStream<Integer> buildOutputStream() {
        return new NumberStringOutputStream(peerAddress, super.streamOrigin.output());
    }
}