package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.BaseGetDataAndHeaderMsg;
import com.nchain.jcl.protocol.messages.GetBlocksMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 09/09/2019
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
