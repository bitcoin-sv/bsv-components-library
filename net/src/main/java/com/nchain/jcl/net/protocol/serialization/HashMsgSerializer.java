package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.HashMsg;

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
        byteReader.waitForBytes(HashMsg.HASH_LENGTH);
        byte[] hashValue = byteReader.read(HashMsg.HASH_LENGTH);
        return HashMsg.builder().hash(hashValue).build();
    }

    @Override
    public void serialize(SerializerContext context, HashMsg message, ByteArrayWriter byteWriter) {
        byteWriter.write(message.getHashBytes());
    }
}
