package com.nchain.jcl.net.protocol.streams;


import com.nchain.jcl.net.network.streams.PeerStream;
import com.nchain.jcl.net.network.streams.PeerStreamImpl;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;


import com.nchain.jcl.net.protocol.streams.deserializer.Deserializer;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStream;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerStreamState;
import com.nchain.jcl.net.protocol.streams.serializer.SerializerStream;
import com.nchain.jcl.net.protocol.streams.serializer.SerializerStreamState;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.config.RuntimeConfig;

import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of a PeerStream that allows to:
 * - Send Bitcoin Messages, convert them into ByteArrayReaders and send them to the next Stream in line
 * - Receive Bitcoin Messages (after converting them from byteArrayReaders) from the next Stream ihn line.
 *
 * So this Stream is used as a Deserializer/Serializer Stream. Any other class using this Stream will only
 * have to deal with Bitcoin Messages, but all the Deserialization/Serialization/low-level stuff will be
 * hidden from them
 */
public class MessageStream extends PeerStreamImpl<BitcoinMsg<?>, ByteArrayReader> implements PeerStream<BitcoinMsg<?>> {

    private RuntimeConfig runtimeConfig;
    private ProtocolBasicConfig protocolBasicConfig;
    private Deserializer deserializer;
    private PeerStream streamOrigin;

    public MessageStream(ExecutorService executor,
                         RuntimeConfig runtimeConfig,
                         ProtocolBasicConfig protocolBasicConfig,
                         Deserializer deserializer,
                         PeerStream<ByteArrayReader> streamOrigin) {
        super(executor, streamOrigin);
        this.runtimeConfig = runtimeConfig;
        this.protocolBasicConfig = protocolBasicConfig;
        this.deserializer = deserializer;
        this.streamOrigin = streamOrigin;
    }
    @Override
    public DeserializerStream buildInputStream() {
        return new DeserializerStream(super.executor, streamOrigin.input(), runtimeConfig, protocolBasicConfig, deserializer);
    }
    @Override
    public SerializerStream buildOutputStream() {
        return new SerializerStream(super.executor, streamOrigin.output(), protocolBasicConfig);
    }

    @Override
    public MessageStreamState getState() {
        return MessageStreamState.builder()
                .inputState((DeserializerStreamState) input().getState())
                .outputState((SerializerStreamState) output().getState())
                .build();
    }

}
