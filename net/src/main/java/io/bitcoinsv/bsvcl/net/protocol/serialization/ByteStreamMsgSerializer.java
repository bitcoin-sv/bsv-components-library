package io.bitcoinsv.bsvcl.net.protocol.serialization;


import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.ByteStreamMsg;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayBuffer;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instance of {@Link ByteStreamMsg} messages. This will simply write/read the raw bytes.
 */
public class ByteStreamMsgSerializer implements MessageSerializer<ByteStreamMsg> {
    private static ByteStreamMsgSerializer instance;

    private ByteStreamMsgSerializer() {}

    public static ByteStreamMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (ByteStreamMsgSerializer.class) {
                instance = new ByteStreamMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public ByteStreamMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        return new ByteStreamMsg(new ByteArrayBuffer(byteReader.getFullContentAndClose()));
    }

    @Override
    public void serialize(SerializerContext context, ByteStreamMsg message, ByteArrayWriter byteWriter) {
        while (message.getContent().size() > 0) {
            long numberOfBytesToExtract = Long.min(Integer.MAX_VALUE, message.getContent().size());
            byteWriter.write(message.getContent().extract((int) numberOfBytesToExtract));
        }
    }
}
