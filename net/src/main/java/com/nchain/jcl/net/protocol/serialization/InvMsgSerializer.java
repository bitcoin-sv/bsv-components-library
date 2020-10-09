package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.InvMessage;
import com.nchain.jcl.net.protocol.messages.InventoryVectorMsg;

import java.util.List;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
