package com.nchain.jcl.network.streams;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.PeerStream;
import com.nchain.jcl.network.config.NetworkConfig;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.streams.*;
import lombok.Getter;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 14:22
 */
public class NIOStream extends StreamEndpointImpl<ByteArrayReader> implements PeerStream {

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
    public OutputStreamDestinationImpl<ByteArrayReader> buildDestination() {
        return new NIOOutputStreamDestination(super.executor, runtimeConfig, networkConfig, peerAddress, key);
    }

}
