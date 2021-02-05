package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.serialization.BitcoinSerializerUtils;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link VarIntMsg} messages
 */
public class VarIntMsgSerializer implements MessageSerializer<VarIntMsg> {

    private static VarIntMsgSerializer instance;

    private VarIntMsgSerializer() { }

    public static VarIntMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (VarIntMsgSerializer.class) {
                instance = new VarIntMsgSerializer();
            }
        }
        return instance;
    }


    public VarIntMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        byteReader.waitForBytes(1);
        long value = BitcoinSerializerUtils.deserializeVarInt(byteReader);
        // We assign the value to the builder:
        return VarIntMsg.builder().value(value).build();
    }

    @Override
    public void serialize(SerializerContext context, VarIntMsg message, ByteArrayWriter byteWriter) {
        BitcoinSerializerUtils.serializeVarInt(message.getValue(), byteWriter);
    }
}
