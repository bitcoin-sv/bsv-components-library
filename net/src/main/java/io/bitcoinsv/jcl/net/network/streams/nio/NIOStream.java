package io.bitcoinsv.jcl.net.network.streams.nio;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.config.NetworkConfig;
import io.bitcoinsv.jcl.net.network.streams.PeerStream;
import io.bitcoinsv.jcl.net.network.streams.PeerStreamImpl;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class implements a Complete Stream (input/output) that also represents a StreamEndPoint, that is,
 * the final step in the Stream chain. This Stream is physically connected to the Source/Destination, which
 * in this case is a SocketChannel connected to that Peer.
 */
public class NIOStream extends PeerStreamImpl<ByteArrayReader, ByteArrayReader> {

    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;
    private PeerAddress peerAddress;
    private SelectionKey key;

    public NIOStream(PeerAddress peerAddress, ExecutorService executor,
                     RuntimeConfig runtimeConfig, NetworkConfig networkConfig,
                     SelectionKey key) {
        super(peerAddress, executor, null);
        this.runtimeConfig = runtimeConfig;
        this.networkConfig = networkConfig;
        this.peerAddress = peerAddress;
        this.key = key;
    }

    @Override
    public NIOInputStream buildInputStream() {
        return new NIOInputStream(peerAddress, super.executor, runtimeConfig, networkConfig, key);
    }

    @Override
    public NIOOutputStream buildOutputStream() {
        return new NIOOutputStream(peerAddress, runtimeConfig, networkConfig, key);
    }

    @Override
    public NIOPeerStreamState getState() {
        return NIOPeerStreamState.builder()
                .inputState((NIOStreamState) input().getState())
                .outputState((NIOStreamState) output().getState())
                .build();
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }
}
