package io.bitcoinsv.jcl.net.unit.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.streams.PeerStream;
import io.bitcoinsv.jcl.net.network.streams.PeerStreamImpl;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-02-11
 */
public class PeerStreamSimulator<T> extends PeerStreamImpl<T,T> implements PeerStream<T> {

    public PeerStreamSimulator(PeerAddress peerAddress, ExecutorService executor) {
        super(peerAddress, executor, null);
    }

    @Override
    public PeerStreamInOutSimulator<T> buildInputStream() {
        return new PeerStreamInOutSimulator<>(peerAddress, executor);
    }
    @Override
    public PeerStreamInOutSimulator<T> buildOutputStream() {
        return new PeerStreamInOutSimulator<>(peerAddress, executor);
    }
    @Override
    public PeerStreamInOutSimulator<T> input() {
        return (PeerStreamInOutSimulator) inputStream;
    }
    @Override
    public PeerStreamInOutSimulator<T> output() {
        return (PeerStreamInOutSimulator) outputStream;
    }
}
