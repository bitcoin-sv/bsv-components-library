package io.bitcoinsv.jcl.net.unit.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerStream;
import io.bitcoinsv.jcl.net.network.streams.PeerStreamImpl;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-02-11
 */
public class PeerStreamSimulator<T> extends PeerStreamImpl<T,T> implements PeerStream<T> {

    public PeerStreamSimulator(PeerAddress peerAddress) {
        super(peerAddress, null);
    }

    @Override
    public PeerStreamInOutSimulator<T> buildInputStream() {
        return new PeerStreamInOutSimulator<>(peerAddress);
    }
    @Override
    public PeerStreamInOutSimulator<T> buildOutputStream() {
        return new PeerStreamInOutSimulator<>(peerAddress);
    }
    @Override
    public PeerInputStream<T> input() {
        return inputStream;
    }
    @Override
    public PeerOutputStream<T> output() {
        return outputStream;
    }
}