package io.bitcoinsv.bsvcl.net.protocol.serialization;


import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.FeeFilterMsg;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link FeeFilterMsg} messages
 */
public class FeeFilterMsgSerializer implements MessageSerializer<FeeFilterMsg> {

    private static FeeFilterMsgSerializer instance;

    // Constructor
    private FeeFilterMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static FeeFilterMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (FeeFilterMsgSerializer.class) {
                instance = new FeeFilterMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public FeeFilterMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        Long fee = byteReader.readInt64LE();
        return FeeFilterMsg.builder().fee(fee).build();
    }

    @Override
    public void serialize(SerializerContext context, FeeFilterMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint64LE(message.getFee());
    }
}
