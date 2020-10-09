package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instance of {@Link HashMsg} messages
 */
public class TxOutPointMsgSerializer implements MessageSerializer<TxOutPointMsg> {

    private static TxOutPointMsgSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private HashMsgSerializer hashMsgSerializer = HashMsgSerializer.getInstance();

    private TxOutPointMsgSerializer() { }

    public static TxOutPointMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (TxOutPointMsgSerializer.class) {
                instance = new TxOutPointMsgSerializer();
            }
        }

        return instance;
    }

    @Override
    public TxOutPointMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        HashMsg hashMsg = hashMsgSerializer.deserialize(context, byteReader);
        HashMsg reverseHash = HashMsg.builder().hash(reverseBytes(hashMsg)).build();

        byteReader.waitForBytes(4);
        long index = byteReader.readUint32();
        TxOutPointMsg txOutPointMsg = TxOutPointMsg.builder().hash(reverseHash).index(index).build();
        return txOutPointMsg;
    }

    @Override
    public void serialize(SerializerContext context, TxOutPointMsg message, ByteArrayWriter byteWriter) {
        byteWriter.write(reverseBytes(message.getHash()));
        byteWriter.writeUint32LE(message.getIndex());
    }

    private byte[] reverseBytes(HashMsg hashMsg) {
        return ByteTools.reverseBytes(hashMsg.getHashBytes());
    }

}
