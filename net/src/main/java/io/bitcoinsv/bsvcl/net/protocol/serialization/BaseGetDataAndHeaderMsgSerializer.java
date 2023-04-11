package io.bitcoinsv.bsvcl.net.protocol.serialization;



import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.BaseGetDataAndHeaderMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link BaseGetDataAndHeaderMsg} messages
 */
public class BaseGetDataAndHeaderMsgSerializer implements MessageSerializer<BaseGetDataAndHeaderMsg> {
    private static BaseGetDataAndHeaderMsgSerializer instance;

    private BaseGetDataAndHeaderMsgSerializer() {}

    public static BaseGetDataAndHeaderMsgSerializer getInstance() {
        if(instance == null) {
            synchronized (BaseGetDataAndHeaderMsgSerializer.class){
                instance = new BaseGetDataAndHeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BaseGetDataAndHeaderMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        long version = byteReader.readUint32();
        VarIntMsg hashCount = VarIntMsgSerializer.getInstance().deserialize(context,byteReader);

        List locaterhashes = new ArrayList<byte[]>();
        for(int i=0; i < hashCount.getValue() ; i++ ){
            locaterhashes.add(readHashMsg(context, byteReader));
        }
        HashMsg stopHash = readHashMsg(context, byteReader);

        BaseGetDataAndHeaderMsg  baseGetDataAndHeaderMsg =  BaseGetDataAndHeaderMsg.builder()
                .version(version)
                .hashCount(hashCount)
                .blockLocatorHash(locaterhashes)
                .hashStop(stopHash)
                .build();

        return baseGetDataAndHeaderMsg;
    }

    @Override
    public void serialize(SerializerContext context, BaseGetDataAndHeaderMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        VarIntMsgSerializer.getInstance().serialize(context, message.getHashCount(), byteWriter);

        // We are not using the HashMsgSerializer for serialize as
        // We have to flip it around, as it's been read off the wire in little endian.
        List<HashMsg> locatorhashes = message.getBlockLocatorHash();
        for(HashMsg locatorHash:locatorhashes) {
            byteWriter.write(locatorHash.getHashBytes());
        }

        byteWriter.write(message.getHashStop().getHashBytes());

    }

    protected HashMsg readHashMsg(DeserializerContext context, ByteArrayReader byteReader)  {
        HashMsg hashMsg =  HashMsg.builder().hash(byteReader.read(HashMsg.HASH_LENGTH)).build();
        return  hashMsg;
    }
}
