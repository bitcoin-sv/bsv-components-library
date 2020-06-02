package com.nchain.jcl.protocol.serialization;



import com.nchain.jcl.protocol.messages.InvMessage;
import com.nchain.jcl.protocol.messages.InventoryVectorMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.List;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
 *
 *  A Serializer for {@link InvMessage} messages
 */
public class InvMsgSerializer implements MessageSerializer<InvMessage> {

    private static InvMsgSerializer instance;

    private InvMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static InvMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (InvMsgSerializer.class) {
                instance = new InvMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public InvMessage deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        List<InventoryVectorMsg> inventoryVectorMsgs = deserializeList(context, byteReader);
        InvMessage invMessage = InvMessage.builder().invVectorMsgList(inventoryVectorMsgs).build();

        return invMessage;
    }

    /**
     * Deserialize InventoryVectorMsg list
     *
     * @param context
     * @param byteReader
     * @return
     */
    protected List<InventoryVectorMsg> deserializeList(DeserializerContext context, ByteArrayReader byteReader) {
        InventoryVectorMsgSerializer invVector = InventoryVectorMsgSerializer.getInstance();
        return invVector.getInventoryVectorMsgs(context, byteReader);
    }

    @Override
    public void serialize(SerializerContext context, InvMessage message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        List<InventoryVectorMsg> inventoryVectorMsgList = message.getInvVectorList();
        serializeList(context, inventoryVectorMsgList , byteWriter);
    }

    /**
     * Serialize Inventory Vector List
     * @param context
     * @param inventoryVectorMsgList
     * @param byteWriter
     */
    protected void serializeList(SerializerContext context, List<InventoryVectorMsg> inventoryVectorMsgList,
                                        ByteArrayWriter byteWriter) {
        for (InventoryVectorMsg inventoryVectorMsg:inventoryVectorMsgList) {
            InventoryVectorMsgSerializer.getInstance().serialize(context, inventoryVectorMsg, byteWriter);
        }
    }


}
