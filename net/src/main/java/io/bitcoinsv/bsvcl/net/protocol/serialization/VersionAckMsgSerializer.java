package io.bitcoinsv.bsvcl.net.protocol.serialization;


import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.VersionAckMsg;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;

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
