package com.nchain.jcl.net.protocol.serialization.common;


import com.nchain.jcl.net.protocol.config.ProtocolVersion;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.net.protocol.serialization.HeaderMsgSerializer;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.bytes.Sha256HashIncremental;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer for {@link BitcoinMsg}
 */
public class BitcoinMsgSerializerImpl implements BitcoinMsgSerializer {

    // Same variables defined that might affect Performance:

    // If a Message is BIGGER than this, then its checksum is calculated in batches, instead of loading the whole
    // byte array in memory. This is safe for memory-consumption standpoint, but it might be slower.
    // THIS VALUE MUST BE < 2GB
    private static final int MSG_SIZE_LIMIT_CHECKSUM_IN_BATCHES = 1_000_000_000; // 1GB

    // Singleton
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
    public HeaderMsg deserializeHeader(DeserializerContext context, ByteArrayReader byteReader) {
                return HeaderMsgSerializer.getInstance().deserialize(context, byteReader);
    }

    @Override
    public <M extends Message> BitcoinMsg<M> deserialize(DeserializerContext context, ByteArrayReader byteReader,
                                                         String msgType) {

        // First we deserialize the Header:
        HeaderMsg headerMsg = HeaderMsgSerializer.getInstance().deserialize(context, byteReader);

        // Now we deserialize the Body:
        DeserializerContext bodyContext = context.toBuilder()
                .maxBytesToRead(headerMsg.getMsgLength())
                .build();
        MessageSerializer<M> bodySerializer = getBodySerializer(msgType);
        M bodyMsg = bodySerializer.deserialize(bodyContext, byteReader);
        BitcoinMsg<M> result = new BitcoinMsg<>(headerMsg, bodyMsg);
        return result;
    }


    public long calculateChecksumOfBigMessage(ByteArrayReader reader) {
        int CHUNK_SIZE = 1_000_000; // 10 MB added at a time

        Sha256HashIncremental shaIncremental = new Sha256HashIncremental();
        while (!reader.isEmpty()) {
            int numBytesToRead = (int) Math.min(reader.size(), CHUNK_SIZE);
            byte[] bytesToAdd = reader.read(numBytesToRead);
            shaIncremental.add(bytesToAdd);
        }
        return Utils.readUint32(shaIncremental.hashTwice(), 0);
    }

    @Override
    public <M extends Message> ByteArrayReader serialize(SerializerContext context, BitcoinMsg<M> bitcoinMessage,
                                                         String msgType) {

        // Some Notes about Deserialization:
        // If the message is >=4GB, the checksum must NOT be calculated. We just Serialize the message as it
        // is
        // If the message is <4GB, then we need to calculate and populate the CHECKSUM in the HEADER of
        // the message, so we need to:
        //  1 - Serialize the Message BODY
        //  2 - Calculate the checksum and put that value into the message Header
        //  3 - Serialize both (header and Body)

        // Original Header:
        HeaderMsg header = bitcoinMessage.getHeader();

        // Writer use for final deserialization:
        ByteArrayWriter finalWriter = new ByteArrayWriter();

        // Control variables:
        final boolean NEED_TO_CALCULATE_CHECKSUM    = header.getMsgLength() < 4_000_000_000L;
        final boolean MSG_FIT_IN_BYTEARRAY          = header.getMsgLength() <= 2_000_000_000;


        if (!NEED_TO_CALCULATE_CHECKSUM) {
            // We serialize the Header and the BODY as they are, without any changes...
            HeaderMsgSerializer.getInstance().serialize(context, header, finalWriter);
            getBodySerializer(msgType).serialize(context, bitcoinMessage.getBody(), finalWriter);
        } else {
            // We calculate the checksum, so we need to Serialize the BODY: We store it in a "bodyByteReader".
            ByteArrayWriter bodyByteWriter = new ByteArrayWriter();
            getBodySerializer(msgType).serialize(context, bitcoinMessage.getBody(), bodyByteWriter);
            ByteArrayReader bodyByteReader = bodyByteWriter.reader(); // BODY Content

            // Checksum Calculation and final serialization:
            if (MSG_FIT_IN_BYTEARRAY) {
                // We store the BODY in an array so we can use it for both the checksum calculation and final
                // serialization...
                byte[] bodyBytes = bodyByteReader.getFullContentAndClose();
                long checksum = Utils.readUint32(Sha256Hash.hashTwice(bodyBytes), 0);
                HeaderMsg headerWithChecksum = header.toBuilder().checksum(checksum).build();
                HeaderMsgSerializer.getInstance().serialize(context, headerWithChecksum, finalWriter);
                finalWriter.write(bodyBytes);
            } else {
                // The Message cannot be stored in an array. So we use the reader to calculate the checksum, and then
                // we need to Serialize the BODY AGAIN...
                long checksum = calculateChecksumOfBigMessage(bodyByteReader);
                HeaderMsg headerWithChecksum = header.toBuilder().checksum(checksum).build();
                HeaderMsgSerializer.getInstance().serialize(context, headerWithChecksum, finalWriter);
                getBodySerializer(msgType).serialize(context, bitcoinMessage.getBody(), finalWriter); // AGAIN
            }
        }

        return finalWriter.reader();
    }

    protected <M extends Message> MessageSerializer<M> getBodySerializer(String msgType) {
        return MsgSerializersFactory.getSerializer(msgType);
    }

}
