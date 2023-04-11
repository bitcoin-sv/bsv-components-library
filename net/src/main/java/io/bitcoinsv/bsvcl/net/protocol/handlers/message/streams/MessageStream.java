package io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams;


import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.Deserializer;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.DeserializerStream;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.DeserializerStreamState;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.serializer.SerializerStream;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.serializer.SerializerStreamState;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.Message;
import io.bitcoinsv.bsvcl.net.network.streams.PeerStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerStreamImpl;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig;


import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;

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
public class MessageStream extends PeerStreamImpl<Message, ByteArrayReader> {

    private RuntimeConfig runtimeConfig;
    private MessageHandlerConfig messageConfig;
    private Deserializer deserializer;
    private PeerStream streamOrigin;
    private ExecutorService dedicatedConnectionsExecutor;
    private LoggerUtil parentLogger;

    public MessageStream(
            RuntimeConfig runtimeConfig,
            MessageHandlerConfig messageConfig,
            Deserializer deserializer,
            PeerStream<ByteArrayReader> streamOrigin,
            ExecutorService dedicatedConnectionsExecutor,
            LoggerUtil parentLogger
    ) {
        super(streamOrigin);
        this.runtimeConfig = runtimeConfig;
        this.messageConfig = messageConfig;
        this.deserializer = deserializer;
        this.streamOrigin = streamOrigin;
        this.dedicatedConnectionsExecutor = dedicatedConnectionsExecutor;
        this.parentLogger = parentLogger;
    }
    @Override
    public DeserializerStream buildInputStream() {
        return new DeserializerStream(streamOrigin.input(), runtimeConfig, messageConfig, deserializer, dedicatedConnectionsExecutor, parentLogger);
    }
    @Override
    public SerializerStream buildOutputStream() {
        return new SerializerStream(streamOrigin.output(), messageConfig, parentLogger);
    }

    @Override
    public MessageStreamState getState() {
        return MessageStreamState.builder()
                .inputState((DeserializerStreamState) input().getState())
                .outputState((SerializerStreamState) output().getState())
                .build();
    }

}