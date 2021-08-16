/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.messages.GetHeadersEnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

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
