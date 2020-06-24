package com.nchain.jcl.protocol.serialization.streams;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.PeerStream;
import com.nchain.jcl.protocol.config.ProtocolConfig;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.protocol.serialization.common.BitcoinMsgSerializerImpl;
import com.nchain.jcl.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.streams.OutputStream;
import com.nchain.jcl.tools.streams.OutputStreamImpl;
import com.nchain.jcl.tools.streams.StreamDataEvent;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author m.fletcher@nchain.com
 * @autor i.fernandez@nchain.com
 *
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-03 10:37
 *
 * This class implements a Serializer Stream that takes a Bitcoin Message as an input, and converts it into a
 * ByteArrayReader, which sent to its destination.
 *
 * The "transform()" function is the main entry oint. This function will carry out the transformation. The result
 * returned by this function will be taken by the parent class and sent to the destination of this class.
 */
public class SerializerStream extends OutputStreamImpl<BitcoinMsg<?>, ByteArrayReader> implements PeerStream {

    private final SerializerContext serializerContext;
    @Getter private SerializerStreamState state;

    /** Constructor.*/
    public SerializerStream(ExecutorService executor,
                            OutputStream<ByteArrayReader> destination,
                            ProtocolConfig protocolConfig) {
        super(executor, destination);
        this.serializerContext = SerializerContext.builder().protocolconfig(protocolConfig).build();

    }

    @Override
    public List<StreamDataEvent<ByteArrayReader>> transform(StreamDataEvent<BitcoinMsg<?>> data) {
        return Arrays.asList(new StreamDataEvent<>(
                BitcoinMsgSerializerImpl.getInstance().serialize(
                        serializerContext,
                        data.getData(),
                        data.getData().getHeader().getCommand())));
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
        if (destination instanceof PeerStream) return ((PeerStream) destination).getPeerAddress();
        else throw new RuntimeException("The Destination of this Stream is NOT connected to a Peer!");
    }
}
