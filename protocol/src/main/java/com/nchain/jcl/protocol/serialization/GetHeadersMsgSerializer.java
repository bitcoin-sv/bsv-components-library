package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.BaseGetDataAndHeaderMsg;
import com.nchain.jcl.protocol.messages.GetHeadersMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 12/09/2019
 */
public class GetHeadersMsgSerializer implements MessageSerializer<GetHeadersMsg> {

    private static GetHeadersMsgSerializer instance;

    private GetHeadersMsgSerializer() { }

    public static GetHeadersMsgSerializer getInstance() {
        if(instance == null) {
            synchronized (GetHeadersMsgSerializer.class){
                instance = new GetHeadersMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public GetHeadersMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        BaseGetDataAndHeaderMsg baseMsg = BaseGetDataAndHeaderMsgSerializer.getInstance().deserialize(
                context, byteReader);
        GetHeadersMsg getHeadersMsg =  GetHeadersMsg.builder().baseGetDataAndHeaderMsg(baseMsg).build();
        return getHeadersMsg;
    }

    @Override
    public void serialize(SerializerContext context, GetHeadersMsg message, ByteArrayWriter byteWriter) {
     BaseGetDataAndHeaderMsg getBaseMsg = message.getBaseGetDataAndHeaderMsg();
     BaseGetDataAndHeaderMsgSerializer.getInstance().serialize(context, getBaseMsg, byteWriter);
    }
}
