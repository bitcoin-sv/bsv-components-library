package io.bitcoinsv.jcl.net.protocol.handlers.message.streams.serializer;

import io.bitcoinsv.jcl.net.network.PeerAddress;

import io.bitcoinsv.jcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStreamImpl;
import io.bitcoinsv.jcl.net.network.streams.StreamDataEvent;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.jcl.net.protocol.serialization.common.*;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.net.tools.LoggerUtil;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author m.fletcher@nchain.com
 * @autor i.fernandez@nchain.com
 *
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class implements a Serializer Stream that takes a Bitcoin Message as an input, and converts it into a
 * ByteArrayReader, which sent to its destination.
 *
 * The "transform()" function is the main entry point. This function will carry out the transformation. The result
 * returned by this function will be taken by the parent class and sent to the destination of this class.
 */

public class SerializerStream extends PeerOutputStreamImpl<Message, ByteArrayReader> {

    // Protocol Configuration
    private MessageHandlerConfig messageConfig;

    // We keep track of the num ber of Msgs processed:
    private BigInteger numMsgs = BigInteger.ZERO;

    // For loggin:
    private LoggerUtil logger;

    /** Constructor.*/
    public SerializerStream(PeerOutputStream<ByteArrayReader> destination,
                            MessageHandlerConfig messageConfig,
                            LoggerUtil parentLogger) {
        super(destination);
        this.logger = (parentLogger == null)
                        ? new LoggerUtil(this.getPeerAddress().toString(), this.getClass())
                        : LoggerUtil.of(parentLogger, "Serializer", this.getClass());
        this.messageConfig = messageConfig;
    }

    /** Constructor.*/
    public SerializerStream(PeerOutputStream<ByteArrayReader> destination,
                            MessageHandlerConfig messageConfig) {
        this(destination, messageConfig, new LoggerUtil("Serializer", SerializerStream.class));
    }

    @Override
    public SerializerStreamState getState() {
        return SerializerStreamState.builder().numMsgs(numMsgs).build();
    }

    @Override
    public List<StreamDataEvent<ByteArrayReader>> transform(StreamDataEvent<Message> data) {
        logger.trace(this.peerAddress, "Serializing " + data.getData().getMessageType().toUpperCase() + " Message...");

        SerializerContext serializerContext = SerializerContext.builder()
                .protocolBasicConfig(messageConfig.getBasicConfig())
                .insideVersionMsg(data.getData().equals(VersionMsg.MESSAGE_TYPE))
                .build();

        List<StreamDataEvent<ByteArrayReader>> result;
        if(data.getData().getMessageType().equals(BitcoinMsg.MESSAGE_TYPE)) {
            BitcoinMsg<?> bitcoinMsg = (BitcoinMsg<?>) data.getData();
            result = Arrays.asList(new StreamDataEvent<>(
                    BitcoinMsgSerializerImpl.getInstance().serialize(
                            serializerContext,
                            bitcoinMsg
                    )));
        } else {
            MessageSerializer serializer = MsgSerializersFactory.getSerializer(data.getData().getMessageType());
            ByteArrayWriter writer = new ByteArrayWriter();

            serializer.serialize(serializerContext, data.getData(), writer);
            result = Arrays.asList(new StreamDataEvent<>(writer.reader()));
        }

        return result;
    }

    /**
     * An outputStream is connected to another OutpusTream, which is usually referred to as "destination". This
     * "destination" in turn might be another OututStream, so we might have a chain of outputStream linked together.
     * The last END of the chain is a "real" Destination (extending OutputStreamDestinationImpl), and that's the ONLY
     * one that is really connected to a PeerAddress.
     *
     * So if the PeerAddress is requested from an OutputStream, we just "pass" this question to its destination. If this
     * destination is just another OutputStream, the question will be passed over and over until it reaches the "real"
     * destination at the end of the chain.
     */
    @Override
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }
}
