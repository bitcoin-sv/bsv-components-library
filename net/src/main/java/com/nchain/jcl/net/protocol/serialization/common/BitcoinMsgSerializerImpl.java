package com.nchain.jcl.net.protocol.serialization.common;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.messages.GetdataMsg;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.net.protocol.serialization.HeaderMsgSerializer;
import com.nchain.jcl.tools.bytes.ByteArrayConfig;
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

    // for performance sake:
    private static long CHECKSUM_EMPTY_MSG = Utils.readUint32(new Sha256HashIncremental().hashTwice(), 0);

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
    public <M extends BodyMessage> M deserializeBody(DeserializerContext context, HeaderMsg headerMsg, ByteArrayReader byteReader) {

        // We check if the checksum of the BODY needs to be calculated first. A checksum needs to be calculated if:
        // - The context says so AND:
        // - The message is NOT "extended" (extended msgs were introduced in 70016) AND:
        boolean needToCalculateChecksum = context.isCalculateChecksum() && !headerMsg.isExtendedMsg();
        long checksum = needToCalculateChecksum ? calculateChecksum(byteReader, headerMsg.getMsgLength()) : 0;

        // We deserialize the Body:
        MessageSerializer<M> bodySerializer = getBodySerializer(headerMsg.getMsgCommand());
        M bodyMsg = bodySerializer.deserialize(context, byteReader);

        // We inject the checksum if needed:
        if (needToCalculateChecksum) {
            bodyMsg = (M) bodyMsg.toBuilder().checksum(checksum).build();
        }

        // In case there are still some bytes in the buffer left AFTER Deserializing the Body, we just read them and
        // store them within the message.
        if (context.getMaxBytesToRead() != null && bodyMsg.getLengthInBytes() < context.getMaxBytesToRead()) {
            int numExtraBytes = (int) (context.getMaxBytesToRead() - bodyMsg.getLengthInBytes());
            byte[] extraBytes = byteReader.read(numExtraBytes);
            bodyMsg = (M) bodyMsg.toBuilder().extraBytes(extraBytes).build();
        }

        return bodyMsg;
    }

    private long calculateChecksum(ByteArrayReader byteReader, long numBytes) {
        final int MAX_BYTES_TO_READ = 2_000_000_000; // 2GB

        // In case the message is empty:
        if (numBytes == 0) { return CHECKSUM_EMPTY_MSG;}

        // Optimization: (message <= 2GB)
        if (numBytes <= MAX_BYTES_TO_READ) {
            return Utils.readUint32(Sha256Hash.hashTwice(byteReader.get((int)numBytes)), 0);
        }

        // For the rest of messages >= 2GB:
        Sha256HashIncremental shaIncremental = new Sha256HashIncremental();
        long numBytesRead = 0;
        while (numBytesRead < numBytes) {
            int numBytesToRead = (int) Math.min(MAX_BYTES_TO_READ, (numBytes - numBytesRead));
            shaIncremental.add(byteReader.get(numBytesRead, numBytesToRead));
            numBytesRead += numBytesToRead;
        }
        return Utils.readUint32(shaIncremental.hashTwice(), 0);
    }

    @Override
    public <M extends BodyMessage> long calculateChecksum(ProtocolBasicConfig protocolBasicConfig, M bodyMessage) {
        ByteArrayWriter writer = new ByteArrayWriter();
        SerializerContext serContext = SerializerContext.builder()
                .protocolBasicConfig(protocolBasicConfig)
                .insideVersionMsg(bodyMessage.getMessageType().equalsIgnoreCase(VersionMsg.MESSAGE_TYPE))
                .build();
        getBodySerializer(bodyMessage.getMessageType()).serialize(serContext, bodyMessage, writer);
        ByteArrayReader bodyMessageReader = writer.reader();
        long checksum = calculateChecksum(bodyMessageReader, bodyMessage.getLengthInBytes());
        bodyMessageReader.closeAndClear();
        return checksum;
    }

    @Override
    public <M extends BodyMessage> BitcoinMsg<M> deserialize(DeserializerContext context, ByteArrayReader byteReader,
                                                         String msgType) {

        // First we deserialize the Header:
        HeaderMsg headerMsg = HeaderMsgSerializer.getInstance().deserialize(context, byteReader);

        // Now we deserialize the Body: We adjust the number of remaining bytes to read, after deserializing the Header:
        if (context.getMaxBytesToRead() != null) {
            long remainingBytes = context.getMaxBytesToRead() - headerMsg.getLengthInBytes();
            context = context.toBuilder().maxBytesToRead(remainingBytes).build();
        }

        M bodyMsg = deserializeBody(context, headerMsg, byteReader);

        // We build a whole BTC Message and return it:
        BitcoinMsg<M> result = new BitcoinMsg<>(headerMsg, bodyMsg);
        return result;
    }

    @Override
    public <M extends BodyMessage> ByteArrayReader serialize(SerializerContext context, BitcoinMsg<M> bitcoinMessage,
                                                         String msgType) {

        // Some Notes about Deserialization:
        // If the message is >=4GB, the checksum must NOT be calculated. We just Serialize the message as it is
        // If the message is <4GB, then we need to calculate and populate the CHECKSUM in the HEADER of
        // the message, so we need to:
        //  1 - Serialize the Message BODY
        //  2 - Calculate the checksum and put that value into the message Header
        //  3 - Serialize both (header and Body)

        // Original Header:
        HeaderMsg header = bitcoinMessage.getHeader();

        // Writer use for final deserialization:
        // NOTE: If the Message is BIG, we configure the ByteArrayWriter accordingly for the sake of performance.
        // For now this is hardcoded here, bu tin the future we¡ll define a proper "SerializerConfig" or a similar
        // approach.
        boolean IS_BIG_MSG = (header.getMsgLength() >= 10_000_000); // 10 MB
        ByteArrayConfig byteArrayConfig = new ByteArrayConfig((IS_BIG_MSG) ? ByteArrayConfig.ARRAY_SIZE_BIG : ByteArrayConfig.ARRAY_SIZE_NORMAL);
        ByteArrayWriter finalWriter = new ByteArrayWriter(byteArrayConfig);

        if (header.isExtendedMsg()) {
            // We serialize the Header and the BODY as they are, without any changes...
            HeaderMsgSerializer.getInstance().serialize(context, header, finalWriter);
            getBodySerializer(msgType).serialize(context, bitcoinMessage.getBody(), finalWriter);
        } else {
            // We calculate the checksum, so we need to Serialize the BODY: We store it in a "bodyByteReader".
            ByteArrayWriter bodyByteWriter = new ByteArrayWriter();
            getBodySerializer(msgType).serialize(context, bitcoinMessage.getBody(), bodyByteWriter);
            ByteArrayReader bodyByteReader = bodyByteWriter.reader(); // BODY Content

            // We calculate the checksum and inject it back into the header:
            long checksum = calculateChecksum(bodyByteReader, header.getMsgLength());
            HeaderMsg headerWithChecksum = header.toBuilder().checksum(checksum).build();

            // We serialize the HEADER:
            HeaderMsgSerializer.getInstance().serialize(context, headerWithChecksum, finalWriter);

            // We serialize the BODY:
            final int SIZE_2GB = 2_000_000_000;
            while (!bodyByteReader.isEmpty()) {
                finalWriter.write(bodyByteReader.read((int) Math.min(SIZE_2GB, bodyByteReader.size())));
            }
        }

        // After serializing the message, if the msgs has some "extraBytes" we also serialize them:
        if (bitcoinMessage.getBody().getExtraBytes().length > 0) {
            finalWriter.write(bitcoinMessage.getBody().getExtraBytes());
        }

        return finalWriter.reader();
    }

    protected <M extends Message> MessageSerializer<M> getBodySerializer(String msgType) {
        return MsgSerializersFactory.getSerializer(msgType);
    }

}
