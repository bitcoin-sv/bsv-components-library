package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.messages.InventoryVectorMsg;
import com.nchain.jcl.net.protocol.messages.NotFoundMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 04/09/2019
 *
 *  * A Serializer for  {@link NotFoundMsg} messages
 */
public class NotFoundMsgSerilaizer implements MessageSerializer<NotFoundMsg> {

    private static NotFoundMsgSerilaizer instance;

    private NotFoundMsgSerilaizer() { }

    public static NotFoundMsgSerilaizer getInstance() {
        if( instance == null) {
            synchronized (NotFoundMsgSerilaizer.class) {
                instance = new NotFoundMsgSerilaizer();
            }
        }
        return instance;
    }

    @Override
    public NotFoundMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        InvMsgSerializer serializer = InvMsgSerializer.getInstance();
        List<InventoryVectorMsg> inventoryVectorMsgs = serializer.deserializeList(context, byteReader);
        VarIntMsg count = VarIntMsg.builder().value(inventoryVectorMsgs.size()).build();
        //Builds both the count and inventory list from the messages
        NotFoundMsg getdataMsg = NotFoundMsg.builder().invVectorMsgList(inventoryVectorMsgs).count(count).build();
        return getdataMsg;
    }

    @Override
    public void serialize(SerializerContext context, NotFoundMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        InvMsgSerializer serializer = InvMsgSerializer.getInstance();
        serializer.serializeList(context, message.getInvVectorList(), byteWriter);
    }
}
