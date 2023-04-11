package io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.serializer;

import io.bitcoinsv.bsvcl.net.protocol.messages.VersionMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.Message;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MsgSerializersFactory;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerOutputStreamImpl;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

import java.math.BigInteger;
import java.util.List;

/**
 * @author m.fletcher@nchain.com
 * @autor i.fernandez@nchain.com
 * <p>
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * This class implements a Serializer Stream that takes a Bitcoin Message as an input, and converts it into a
 * ByteArrayReader, which sent to its destination.
 * <p>
 * The "transform()" function is the main entry point. This function will carry out the transformation. The result
 * returned by this function will be taken by the parent class and sent to the destination of this class.
 */

public class SerializerStream extends PeerOutputStreamImpl<Message, ByteArrayReader> {

    // Protocol Configuration
    private final MessageHandlerConfig messageConfig;

    // We keep track of the num ber of Msgs processed:
    private final BigInteger numMsgs = BigInteger.ZERO;

    // For loggin:
    private final LoggerUtil logger;

    public SerializerStream(
            PeerOutputStream<ByteArrayReader> destination,
            MessageHandlerConfig messageConfig,
            LoggerUtil parentLogger
    ) {
        super(destination);
        this.logger = (parentLogger == null)
                ? new LoggerUtil(this.getPeerAddress().toString(), this.getClass())
                : LoggerUtil.of(parentLogger, "Serializer", this.getClass());
        this.messageConfig = messageConfig;
    }

    public SerializerStream(PeerOutputStream<ByteArrayReader> destination,
                            MessageHandlerConfig messageConfig) {
        this(destination, messageConfig, new LoggerUtil("Serializer", SerializerStream.class));
    }

    @Override
    public SerializerStreamState getState() {
        return SerializerStreamState.builder().numMsgs(numMsgs).build();
    }

    @Override
    public List<ByteArrayReader> transform(Message data) {
        logger.trace(this.peerAddress, "Serializing " + data.getMessageType().toUpperCase() + " Message...");

        SerializerContext serializerContext = SerializerContext.builder()
                .protocolBasicConfig(messageConfig.getBasicConfig())
                .insideVersionMsg(data.getMessageType().equals(VersionMsg.MESSAGE_TYPE))
                .build();

        if (data.getMessageType().equals(BitcoinMsg.MESSAGE_TYPE)) {
            BitcoinMsg<?> bitcoinMsg = (BitcoinMsg<?>) data;

            return List.of(
                    BitcoinMsgSerializerImpl.getInstance().serialize(
                            serializerContext,
                            bitcoinMsg
                    )
            );
        }

        MessageSerializer serializer = MsgSerializersFactory.getSerializer(data.getMessageType());
        ByteArrayWriter writer = new ByteArrayWriter();

        serializer.serialize(serializerContext, data, writer);

        return List.of(writer.reader());
    }

    /**
     * An outputStream is connected to another OutpusTream, which is usually referred to as "destination". This
     * "destination" in turn might be another OututStream, so we might have a chain of outputStream linked together.
     * The last END of the chain is a "real" Destination (extending OutputStreamDestinationImpl), and that's the ONLY
     * one that is really connected to a PeerAddress.
     * <p>
     * So if the PeerAddress is requested from an OutputStream, we just "pass" this question to its destination. If this
     * destination is just another OutputStream, the question will be passed over and over until it reaches the "real"
     * destination at the end of the chain.
     */
    @Override
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }
}