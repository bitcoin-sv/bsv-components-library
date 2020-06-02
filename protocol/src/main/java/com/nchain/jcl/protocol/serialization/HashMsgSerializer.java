package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.HashMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.bytes.HEX;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 18/09/2019
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
