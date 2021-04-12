package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.SendCompactBlockMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class SendCompactBlockMsgSerializer implements MessageSerializer<SendCompactBlockMsg> {
    private static SendCompactBlockMsgSerializer instance;

    public static SendCompactBlockMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (SendCompactBlockMsgSerializer.class) {
                instance = new SendCompactBlockMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public SendCompactBlockMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        return SendCompactBlockMsg.builder()
            .highBandwidthRelaying(byteReader.readBoolean())
            .version(byteReader.readInt64LE())
            .build();
    }

    @Override
    public void serialize(SerializerContext context, SendCompactBlockMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeBoolean(message.isHighBandwidthRelaying());
        byteWriter.writeUint64LE(message.getVersion());
    }
}
