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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-03 10:37
 */
public class ByteArrayReaderOutputStream extends OutputStreamImpl<BitcoinMsg<?>, ByteArrayReader> implements PeerStream {

    private final SerializerContext serializerContext;
    private final PeerAddress peerAddress;

    /**
     * Constructor.
     *
     * @param executor    The transformation on the data is executed on blocking/non-blocking mode depending on this. If
     *                    null, the data is processed in blocking mode. If not null, the data will be processed in
     *                    a separate Thread/s. on a Single Thread, the data will be processed in the same order its
     *                    received. With more Threads the data will be processed concurrently and the order cannot be
     *                    guaranteed.
     * @param destination The Output Stream that is linked to this OutputStream.
     * @param peerAddress The Peer Address that this output stream is connected too
     * @param protocolConfig The Protocol Configuration which the output stream serializtion will use
     *
     */
    public ByteArrayReaderOutputStream(ExecutorService executor,
                                       OutputStream<ByteArrayReader> destination,
                                       PeerAddress peerAddress,
                                       ProtocolConfig protocolConfig) {
        super(executor, destination);
        this.serializerContext = SerializerContext.builder().protocolconfig(protocolConfig).build();
        this.peerAddress = peerAddress;
    }

    @Override
    public List<StreamDataEvent<ByteArrayReader>> transform(StreamDataEvent<BitcoinMsg<?>> data) {
        return Arrays.asList(new StreamDataEvent<>(
                BitcoinMsgSerializerImpl.getInstance().serialize(
                        serializerContext,
                        data.getData(),
                        data.getData().getHeader().getCommand())));
    }

    @Override
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }
}
