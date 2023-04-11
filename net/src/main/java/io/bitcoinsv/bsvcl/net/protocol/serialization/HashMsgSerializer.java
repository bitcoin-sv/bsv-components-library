package io.bitcoinsv.bsvcl.net.protocol.serialization;



import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instances of  {@link HashMsg} messages
 */
public class HashMsgSerializer implements MessageSerializer<HashMsg> {
    private static HashMsgSerializer instance;

    private HashMsgSerializer() { }

    public static HashMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (HashMsgSerializer.class) {
                instance = new HashMsgSerializer();
            }
        }

        return instance;
    }

    @Override
    public HashMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        byte[] hashValue = byteReader.read(HashMsg.HASH_LENGTH);
        return HashMsg.builder().hash(hashValue).build();
    }

    @Override
    public void serialize(SerializerContext context, HashMsg message, ByteArrayWriter byteWriter) {
        byteWriter.write(message.getHashBytes());
    }
}
