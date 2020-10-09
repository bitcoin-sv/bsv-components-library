package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg;
import com.nchain.jcl.net.protocol.messages.GetBlocksMsg;

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
