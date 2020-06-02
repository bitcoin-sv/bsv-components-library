package com.nchain.jcl.protocol.serialization;



import com.nchain.jcl.protocol.messages.GetdataMsg;
import com.nchain.jcl.protocol.messages.InventoryVectorMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 27/08/2019
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
