/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;



import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

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
