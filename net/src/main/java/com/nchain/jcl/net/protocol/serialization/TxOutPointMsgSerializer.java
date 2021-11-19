package com.nchain.jcl.net.protocol.serialization;



import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

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

        long index = byteReader.readUint32();
        TxOutPointMsg txOutPointMsg = TxOutPointMsg.builder().hash(hashMsg).index(index).build();
        return txOutPointMsg;
    }

    @Override
    public void serialize(SerializerContext context, TxOutPointMsg message, ByteArrayWriter byteWriter) {
        byteWriter.write(message.getHash().getHashBytes());
        byteWriter.writeUint32LE(message.getIndex());
    }


}
