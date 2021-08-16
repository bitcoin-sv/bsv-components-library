/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;



import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.messages.InventoryVectorMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
        byteWriter.write(message.getHashMsg().getHashBytes());
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
