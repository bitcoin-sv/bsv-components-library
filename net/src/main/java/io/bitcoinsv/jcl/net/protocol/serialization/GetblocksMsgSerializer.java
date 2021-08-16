/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.GetBlocksMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *
 * A Serializer for {@link GetblocksMsgSerializer} messages
 */
public class GetblocksMsgSerializer implements MessageSerializer<GetBlocksMsg> {

    private static GetblocksMsgSerializer instance;

    private GetblocksMsgSerializer() { }

    public static GetblocksMsgSerializer getInstance() {
        if(instance == null) {
            synchronized (GetblocksMsgSerializer.class){
                instance = new GetblocksMsgSerializer();
            }
        }
        return instance;
    }


    @Override
    public GetBlocksMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializer.getInstance().deserialize(
                context, byteReader);
        GetBlocksMsg getBlocksMsg = GetBlocksMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build();
        return getBlocksMsg;
    }

    @Override
    public void serialize(SerializerContext context, GetBlocksMsg message, ByteArrayWriter byteWriter) {
        BaseGetDataAndHeaderMsg getBaseMsg = message.getBaseGetDataAndHeaderMsg();
        BaseGetDataAndHeaderMsgSerializer.getInstance().serialize(context, getBaseMsg, byteWriter);
    }
}
