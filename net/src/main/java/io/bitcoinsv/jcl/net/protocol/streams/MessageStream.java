/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.streams;


import io.bitcoinsv.jcl.net.network.streams.PeerStream;
import io.bitcoinsv.jcl.net.network.streams.PeerStreamImpl;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;


import io.bitcoinsv.jcl.net.protocol.streams.deserializer.Deserializer;
import io.bitcoinsv.jcl.net.protocol.streams.deserializer.DeserializerStream;
import io.bitcoinsv.jcl.net.protocol.streams.deserializer.DeserializerStreamState;
import io.bitcoinsv.jcl.net.protocol.streams.serializer.SerializerStream;
import io.bitcoinsv.jcl.net.protocol.streams.serializer.SerializerStreamState;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.config.RuntimeConfig;

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
    private ExecutorService dedicatedConnectionsExecutor;

    public MessageStream(ExecutorService eventBusExecutor,
                         RuntimeConfig runtimeConfig,
                         ProtocolBasicConfig protocolBasicConfig,
                         Deserializer deserializer,
                         PeerStream<ByteArrayReader> streamOrigin,
                         ExecutorService dedicatedConnectionsExecutor) {
        super(eventBusExecutor, streamOrigin);
        this.runtimeConfig = runtimeConfig;
        this.protocolBasicConfig = protocolBasicConfig;
        this.deserializer = deserializer;
        this.streamOrigin = streamOrigin;
        this.dedicatedConnectionsExecutor = dedicatedConnectionsExecutor;
    }
    @Override
    public DeserializerStream buildInputStream() {
        return new DeserializerStream(super.executor, streamOrigin.input(), runtimeConfig, protocolBasicConfig, deserializer, dedicatedConnectionsExecutor);
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
