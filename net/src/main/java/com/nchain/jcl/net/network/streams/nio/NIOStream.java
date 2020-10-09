package com.nchain.jcl.net.network.streams.nio;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.base.tools.streams.*;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.config.NetworkConfig;
import com.nchain.jcl.net.network.streams.PeerInputStream;
import com.nchain.jcl.net.network.streams.PeerOutputStream;
import com.nchain.jcl.net.network.streams.PeerStream;
import lombok.Getter;

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
public class NIOStream extends StreamEndpointImpl<ByteArrayReader> implements PeerStream<ByteArrayReader> {

    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;
    @Getter private PeerAddress peerAddress;
    private SelectionKey key;

    @Getter private NIOStreamState state;

    public NIOStream(ExecutorService executor, RuntimeConfig runtimeConfig, NetworkConfig networkConfig,
                     PeerAddress peerAddress, SelectionKey key) {
        super(executor);
        this.runtimeConfig = runtimeConfig;
        this.networkConfig = networkConfig;
        this.peerAddress = peerAddress;
        this.key = key;
    }

    @Override
    public InputStreamSourceImpl<ByteArrayReader> buildSource() {
        return new NIOInputStreamSource(super.executor, runtimeConfig, networkConfig, peerAddress, key);
    }

    @Override
    public OutputStreamDestinationImpl<ByteArrayReader> buildDestination() {
        return new NIOOutputStreamDestination(super.executor, runtimeConfig, networkConfig, peerAddress, key);
    }

    @Override
    public PeerInputStream<ByteArrayReader> input() {
        return (PeerInputStream<ByteArrayReader> ) super.source;
    }

    @Override
    public PeerOutputStream<ByteArrayReader> output() {
        return (PeerOutputStream<ByteArrayReader>) super.destination;
    }

    @Override
    public NIOStreamState getState() {
        return NIOStreamState.builder()
                .inputState((NIOInputStreamState) input().getState())
                .outputState((NIOOutputStreamState) output().getState())
                .build();
    }

}
