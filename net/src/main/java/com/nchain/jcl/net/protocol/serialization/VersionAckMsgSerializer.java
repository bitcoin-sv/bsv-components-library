package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.VersionAckMsg;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer for {@link VersionAckMsg} messages
 */
public class VersionAckMsgSerializer implements MessageSerializer<VersionAckMsg> {

    private static VersionAckMsgSerializer instance;

    private VersionAckMsgSerializer() { }

    /** Returns the instance of this Class (Singleton) */
    public static VersionAckMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (VersionAckMsgSerializer.class) {
                instance = new VersionAckMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public  VersionAckMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        return VersionAckMsg.builder().build();
    }

    @Override
    public  void serialize(SerializerContext context, VersionAckMsg message, ByteArrayWriter byteWriter) {
        // Empty Message
    }
}