package io.bitcoinsv.bsvcl.net.protocol.serialization;


import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.GetHeadersEnMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link GetHeadersEnMsg} messages
 */
public class GetHeadersEnMsgSerializer implements MessageSerializer<GetHeadersEnMsg> {
    private static GetHeadersEnMsgSerializer instance;

    public static GetHeadersEnMsgSerializer getInstance() {
        if(instance == null) {
            synchronized (GetHeadersMsgSerializer.class){
                instance = new GetHeadersEnMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public GetHeadersEnMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        long version = byteReader.readUint32();
        HashMsg blockLocatorHash = BaseGetDataAndHeaderMsgSerializer.getInstance().readHashMsg(context, byteReader);
        HashMsg stopHash =BaseGetDataAndHeaderMsgSerializer.getInstance().readHashMsg(context, byteReader);

        GetHeadersEnMsg getHeadersenMsg = GetHeadersEnMsg.builder()
                .version(version)
                .blockLocatorHash(blockLocatorHash)
                .hashStop(stopHash).build();

        return getHeadersenMsg;
    }

    @Override
    public void serialize(SerializerContext context, GetHeadersEnMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        byteWriter.write(message.getBlockLocatorHash().getHashBytes());
        byteWriter.write(message.getHashStop().getHashBytes());
    }
}
