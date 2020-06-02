package com.nchain.jcl.protocol.serialization;



import com.nchain.jcl.protocol.messages.HashMsg;
import com.nchain.jcl.protocol.messages.InventoryVectorMsg;
import com.nchain.jcl.protocol.messages.VarIntMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteTools;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 12/08/2019
 *
 * A Serializer for {@link InventoryVectorMsg} messages
 */
public class InventoryVectorMsgSerializer implements MessageSerializer<InventoryVectorMsg> {

    private static InventoryVectorMsgSerializer instance;


    private InventoryVectorMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static InventoryVectorMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (InventoryVectorMsgSerializer.class) {
                instance = new InventoryVectorMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public InventoryVectorMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        InventoryVectorMsg.VectorType type = InventoryVectorMsg.VectorType.fromCode((int) byteReader.readUint32());
        HashMsg hashMsg  = HashMsgSerializer.getInstance().deserialize(context,byteReader);

        InventoryVectorMsg inventoryVectorMsg = InventoryVectorMsg.builder().type(type).hashMsg(hashMsg).build();
        return inventoryVectorMsg;
    }

    @Override
    public void serialize(SerializerContext context, InventoryVectorMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getType().getValue());
        byteWriter.write(ByteTools.reverseBytes(message.getHashMsg().getHashBytes()));
    }

    /**
     * Returns the list of Inventory Vector list
     *
     * @param context
     * @param byteReader
     * @return
     */
    public  List<InventoryVectorMsg> getInventoryVectorMsgs
            (DeserializerContext context, ByteArrayReader byteReader) {

        VarIntMsg count = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        InventoryVectorMsg invMsg;
        List<InventoryVectorMsg> inventoryVectorMsgs = new ArrayList<>();

        for(int i =0 ; i < count.getValue(); i++) {
            invMsg = InventoryVectorMsgSerializer.getInstance().deserialize(context, byteReader);
            inventoryVectorMsgs.add(invMsg);
        }
        return inventoryVectorMsgs;
    }
}
