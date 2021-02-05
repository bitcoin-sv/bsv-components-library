package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.GetHeadersEnMsg;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Utils;

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
        byteReader.waitForBytes(4);
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
        byteWriter.write(Utils.reverseBytes(message.getBlockLocatorHash().getHashBytes()));
        byteWriter.write(Utils.reverseBytes(message.getHashStop().getHashBytes()));
    }
}
