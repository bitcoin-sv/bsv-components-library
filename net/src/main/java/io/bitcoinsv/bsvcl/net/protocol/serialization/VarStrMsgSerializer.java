package io.bitcoinsv.bsvcl.net.protocol.serialization;

import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link VarStrMsg} messages
 */
public class VarStrMsgSerializer implements MessageSerializer<VarStrMsg> {

    private static VarStrMsgSerializer instance;

    private VarStrMsgSerializer() { }

    public static VarStrMsgSerializer getinstance() {
        if (instance == null) {
            synchronized (VarStrMsgSerializer.class) {
                instance = new VarStrMsgSerializer();
            }
        }
        return instance;
    }

    public  VarStrMsg  deserialize(byte[] bytes) {
        return deserialize(new ByteArrayReader(bytes), bytes.length);
    }

    public  VarStrMsg deserialize(ByteArrayReader byteReader, int length) {
        String str = (length == 0) ? "" : byteReader.readString(length, "UTF-8");

        return VarStrMsg.builder().str(str).build();
    }

    @Override
    public  VarStrMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        // the first part of the message is a VarInt, containing the length of the String:
        VarIntMsg lengthStrMsg = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        int length = (int) lengthStrMsg.getValue();

        // The second part is the String itself:
        String str = (lengthStrMsg.getValue() == 0) ? "" : byteReader.readString(length, "UTF-8");

        return VarStrMsg.builder().str(str).build();
    }

    @Override
    public void serialize(SerializerContext context, VarStrMsg message, ByteArrayWriter byteWriter) {
        // First we serialize the length of the Strg, which its itself a VarIntMsg:
        VarIntMsgSerializer.getInstance().serialize(context, message.getStrLength(), byteWriter);

        // Now we serialize the String itself:
        byteWriter.writeStr(message.getStr(), "UTF-8");
    }
}
