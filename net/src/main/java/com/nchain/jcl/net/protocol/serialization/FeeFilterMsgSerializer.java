package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.FeeFilterMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

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
