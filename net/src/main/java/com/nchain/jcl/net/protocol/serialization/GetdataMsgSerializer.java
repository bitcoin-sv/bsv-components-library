package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.GetdataMsg;
import com.nchain.jcl.net.protocol.messages.InventoryVectorMsg;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *  A Serializer for {@link GetdataMsg} messages
 */
public class GetdataMsgSerializer  implements MessageSerializer<GetdataMsg> {

    private static GetdataMsgSerializer instance;

    private GetdataMsgSerializer() { }

    public static GetdataMsgSerializer getInstance() {
        if( instance == null) {
            synchronized (GetdataMsgSerializer.class) {
                instance = new GetdataMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public GetdataMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        InvMsgSerializer serializer = InvMsgSerializer.getInstance();
        List<InventoryVectorMsg> inventoryVectorMsgs = serializer.deserializeList(context, byteReader);

        //Builds both the count and inventory list from the messages
        GetdataMsg getdataMsg = GetdataMsg.builder().invVectorList(inventoryVectorMsgs).build();
        return getdataMsg;
    }

    @Override
    public void serialize(SerializerContext context, GetdataMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        InvMsgSerializer serializer = InvMsgSerializer.getInstance();
        serializer.serializeList(context, message.getInvVectorList(), byteWriter);
    }
}
