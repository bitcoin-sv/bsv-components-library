package com.nchain.jcl.net.protocol.streams.serializer;

import com.nchain.jcl.net.network.PeerAddress;

import com.nchain.jcl.net.network.streams.PeerOutputStream;
import com.nchain.jcl.net.network.streams.PeerOutputStreamImpl;
import com.nchain.jcl.net.network.streams.StreamDataEvent;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.net.tools.LoggerUtil;

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

public class SerializerStream extends PeerOutputStreamImpl<BitcoinMsg<?>, ByteArrayReader> implements PeerOutputStream<BitcoinMsg<?>> {

    // Protocol Configuration
    private ProtocolBasicConfig ProtocolBasicConfig;

    // We keep track of the num ber of Msgs processed:
    private BigInteger numMsgs = BigInteger.ZERO;

    // For loggin:
    private LoggerUtil logger;

    /** Constructor.*/
    public SerializerStream(ExecutorService executor,
                            PeerOutputStream<ByteArrayReader> destination,
                            ProtocolBasicConfig ProtocolBasicConfig,
                            LoggerUtil parentLogger) {
        super(executor, destination);
        this.logger = (parentLogger == null)
                        ? new LoggerUtil(this.getPeerAddress().toString(), this.getClass())
                        : LoggerUtil.of(parentLogger, "Ser Stream", this.getClass());
        this.ProtocolBasicConfig = ProtocolBasicConfig;
    }

    @Override
    public SerializerStreamState getState() {
        return SerializerStreamState.builder().numMsgs(numMsgs).build();
    }

    @Override
    public List<StreamDataEvent<ByteArrayReader>> transform(StreamDataEvent<BitcoinMsg<?>> data) {
        logger.trace(this.peerAddress, "Serializing " + data.getData().getHeader().getMsgCommand().toUpperCase() + " Message...");

        SerializerContext serializerContext = SerializerContext.builder()
                .protocolBasicConfig(ProtocolBasicConfig)
                .insideVersionMsg(data.getData().is(VersionMsg.MESSAGE_TYPE))
                .build();
        List<StreamDataEvent<ByteArrayReader>> result = Arrays.asList(new StreamDataEvent<>(
                BitcoinMsgSerializerImpl.getInstance().serialize(
                        serializerContext,
                        data.getData(),
                        data.getData().getHeader().getMsgCommand())));
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
