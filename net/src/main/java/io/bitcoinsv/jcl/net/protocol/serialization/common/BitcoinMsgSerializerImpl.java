/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization.common;


import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.Message;
import io.bitcoinsv.jcl.net.protocol.serialization.HeaderMsgSerializer;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer for {@link BitcoinMsg}
 */
public class BitcoinMsgSerializerImpl implements BitcoinMsgSerializer {

    private static BitcoinMsgSerializerImpl instance;

    // Constructor
    protected BitcoinMsgSerializerImpl() { }

    /** Returns the instance of this Class (Singleton) */
    public static BitcoinMsgSerializerImpl getInstance() {
        if (instance == null) {
            synchronized (BitcoinMsgSerializerImpl.class) {
                instance = new BitcoinMsgSerializerImpl();
            }
        }
        return instance;
    }


    @Override
    public int getHeaderLength() {
        return (int) HeaderMsg.MESSAGE_LENGTH;
    }

    @Override
    public HeaderMsg deserializeHeader(DeserializerContext context, ByteArrayReader byteReader) {
                return HeaderMsgSerializer.getInstance().deserialize(context, byteReader);
    }

    @Override
    public <M extends Message> BitcoinMsg<M> deserialize(DeserializerContext context, ByteArrayReader byteReader,
                                                         String command) {

        // First we deserialize the Header:
        DeserializerContext headerContext = context.toBuilder()
                .maxBytesToRead(HeaderMsg.MESSAGE_LENGTH)
                .build();
        HeaderMsg headerMsg = HeaderMsgSerializer.getInstance().deserialize(headerContext, byteReader);

        // Now we deserialize the Body:
        DeserializerContext bodyContext = context.toBuilder()
                .maxBytesToRead(headerMsg.getLength())
                .build();
        MessageSerializer<M> bodySerializer = getBodySerializer(command);

        M bodyMsg = bodySerializer.deserialize(bodyContext, byteReader);

        BitcoinMsg<M> result = new BitcoinMsg<>(headerMsg, bodyMsg);
        return result;
    }

    @Override
    public <M extends Message> ByteArrayReader serialize(SerializerContext context, BitcoinMsg<M> bitcoinMessage,
                                                         String command) {

        // We first serialize the Body and we keep the content, since we'll need it later on
        // when processing the HeaderMsg:

        MessageSerializer<M> bodySerializer = getBodySerializer(command);

        ByteArrayWriter bodyByteWriter = new ByteArrayWriter();

        bodySerializer.serialize(context, bitcoinMessage.getBody(), bodyByteWriter);
        byte[] bodyBytes = bodyByteWriter.reader().getFullContentAndClose(); // TODO: CAREFUL

        // Now we serialize the HeaderMsg.
        // We calculate the value of the "checksum" field of the HeaderMsg at this point.
        // Crypto or hashing operations are computational-consuming tasks, so we only perform some of them
        // when they are really needed. In the case of the Checksum, we calculate it here before serialization:

        long checksum = Utils.readUint32(Sha256Hash.hashTwice(bodyBytes), 0);

        // We need to inject the checksum into the HeaderMsg. But the BitcoinMsg object is immutable, so we need
        // to build another HeaderMsg taking the original one as a reference:
        HeaderMsg header = bitcoinMessage.getHeader();
        HeaderMsg headerWithChecksum = HeaderMsg.builder().checksum(checksum).command(
                header.getCommand()).length(bodyBytes.length).magic(header.getMagic()).build();

        // We serialize the Header:
        ByteArrayWriter finalWriter = new ByteArrayWriter();

        HeaderMsgSerializer.getInstance().serialize(context, headerWithChecksum, finalWriter);

        // We serialize the Body:
        finalWriter.write(bodyBytes);
        return finalWriter.reader();
    }

    protected <M extends Message> MessageSerializer<M> getBodySerializer(String command) {
        return MsgSerializersFactory.getSerializer(command);
    }

}
