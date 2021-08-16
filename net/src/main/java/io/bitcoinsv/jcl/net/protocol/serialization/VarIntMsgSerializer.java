/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.jcl.tools.serialization.BitcoinSerializerUtils;

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
        long value = BitcoinSerializerUtils.deserializeVarInt(byteReader);
        // We assign the value to the byteArray:
        return VarIntMsg.builder().value(value).build();
    }

    @Override
    public void serialize(SerializerContext context, VarIntMsg message, ByteArrayWriter byteWriter) {
        BitcoinSerializerUtils.serializeVarInt(message.getValue(), byteWriter);
    }
}
