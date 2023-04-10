package io.bitcoinsv.bsvcl.net.protocol.serialization;



import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.NotFoundMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for  {@link NotFoundMsg} messages
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
        NotFoundMsg getdataMsg = NotFoundMsg.builder().invVectorMsgList(inventoryVectorMsgs).build();
        return getdataMsg;
    }

    @Override
    public void serialize(SerializerContext context, NotFoundMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        InvMsgSerializer serializer = InvMsgSerializer.getInstance();
        serializer.serializeList(context, message.getInvVectorList(), byteWriter);
    }
}
