package com.nchain.jcl.protocol.streams;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.streams.*;
import com.nchain.jcl.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.config.RuntimeConfig;


import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 16:07
 *
 * An implementation of a PeerStream that allows to:
 * - Send Bitcoin Messages, convert them into ByteArrayReaders and send them to the next Stream in line
 * - Receive Bitcoin Messages (after converting them from byteArrayReaders) from the next Stream ihn line.
 *
 * So this Stream is used as a Deserializer/Serializer Stream. Any other class using this Stream will only
 * have to deal with Bitcoin Messages, but all the Deserialization/Serialization/low-level stuff will ne
 * hidden from them
 */
public class MessageStream extends PeerStreamImpl<BitcoinMsg<?>, ByteArrayReader> implements PeerStream<BitcoinMsg<?>> {

    private RuntimeConfig runtimeConfig;
    private ProtocolBasicConfig protocolBasicConfig;
    private PeerStream streamOrigin;

    public MessageStream(ExecutorService executor, RuntimeConfig runtimeConfig, ProtocolBasicConfig protocolBasicConfig, PeerStream<ByteArrayReader> streamOrigin) {
        super(executor, streamOrigin);
        this.runtimeConfig = runtimeConfig;
        this.protocolBasicConfig = protocolBasicConfig;
        this.streamOrigin = streamOrigin;
    }
    @Override
    public PeerInputStream<BitcoinMsg<?>> buildInputStream() {
        return new DeserializerStream(super.executor, streamOrigin.input(), runtimeConfig, protocolBasicConfig);
    }
    @Override
    public PeerOutputStream<BitcoinMsg<?>> buildOutputStream() {
        return new SerializerStream(super.executor, streamOrigin.output(), protocolBasicConfig);
    }
    @Override
    public PeerAddress getPeerAddress() {
        if (streamOrigin instanceof PeerStreamInfo) return ((PeerStreamInfo) streamOrigin).getPeerAddress();
        else throw new RuntimeException("This Stream is NOT connected to a Peer Stream");
    }
    @Override
    public MessageStreamState getState() {
        return MessageStreamState.builder()
                .inputState((DeserializerStreamState) input().getState())
                .outputState((SerializerStreamState) output().getState())
                .build();
    }

}
