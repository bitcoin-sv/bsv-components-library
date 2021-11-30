package com.nchain.jcl.net.protocol.handlers.message.streams;


import com.nchain.jcl.net.network.streams.PeerStream;
import com.nchain.jcl.net.network.streams.PeerStreamImpl;
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;


import com.nchain.jcl.net.protocol.handlers.message.streams.deserializer.Deserializer;
import com.nchain.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerStream;
import com.nchain.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerStreamState;
import com.nchain.jcl.net.protocol.handlers.message.streams.serializer.SerializerStream;
import com.nchain.jcl.net.protocol.handlers.message.streams.serializer.SerializerStreamState;
import com.nchain.jcl.net.tools.LoggerUtil;
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
    private MessageHandlerConfig messageConfig;
    private Deserializer deserializer;
    private PeerStream streamOrigin;
    private ExecutorService dedicatedConnectionsExecutor;
    private LoggerUtil parentLogger;

    public MessageStream(ExecutorService eventBusExecutor,
                         RuntimeConfig runtimeConfig,
                         MessageHandlerConfig messageConfig,
                         Deserializer deserializer,
                         PeerStream<ByteArrayReader> streamOrigin,
                         ExecutorService dedicatedConnectionsExecutor,
                         LoggerUtil parentLogger) {
        super(eventBusExecutor, streamOrigin);
        this.runtimeConfig = runtimeConfig;
        this.messageConfig = messageConfig;
        this.deserializer = deserializer;
        this.streamOrigin = streamOrigin;
        this.dedicatedConnectionsExecutor = dedicatedConnectionsExecutor;
        this.parentLogger = parentLogger;
    }
    @Override
    public DeserializerStream buildInputStream() {
        return new DeserializerStream(super.executor, streamOrigin.input(), runtimeConfig, messageConfig, deserializer, dedicatedConnectionsExecutor, parentLogger);
    }
    @Override
    public SerializerStream buildOutputStream() {
        return new SerializerStream(super.executor, streamOrigin.output(), messageConfig, parentLogger);
    }

    @Override
    public MessageStreamState getState() {
        return MessageStreamState.builder()
                .inputState((DeserializerStreamState) input().getState())
                .outputState((SerializerStreamState) output().getState())
                .build();
    }

}
