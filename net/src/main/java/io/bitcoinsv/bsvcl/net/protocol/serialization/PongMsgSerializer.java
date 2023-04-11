package io.bitcoinsv.bsvcl.net.protocol.serialization;



import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.PongMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class PongMsgSerializer implements MessageSerializer<PongMsg> {

    private static PongMsgSerializer instance;

    private PongMsgSerializer() { }

    /** Returns the instance of this Serializer (Singleton) */
    public static PongMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (PongMsgSerializer.class) {
                instance = new PongMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public PongMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        long nonce = byteReader.readInt64LE();
        return  PongMsg.builder().nonce(nonce).build();
    }

    @Override
    public void serialize(SerializerContext context, PongMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint64LE(message.getNonce());

    }
}
