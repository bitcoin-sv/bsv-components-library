package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer for {@link HeadersenMsg} messages
 */
public class HeadersenMsgSerializer  implements MessageSerializer<HeadersenMsg> {
    private static HeadersenMsgSerializer instance;

    private HeadersenMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static HeadersenMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (HeadersenMsgSerializer.class) {
                instance = new HeadersenMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public HeadersenMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        List<BlockHeaderEnrichedMsg> blockHeaderEnrichedMsgs = deserializeBlockHeaderenMsgs(context, byteReader);
        HeadersenMsg headersenMsg = HeadersenMsg.builder().blockHeaderEnMsgList(blockHeaderEnrichedMsgs).build();
        return headersenMsg;
    }

    @Override
    public void serialize(SerializerContext context, HeadersenMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        List<BlockHeaderEnrichedMsg> blockHeaderenMsgs = message.getBlockHeaderEnMsgList();
        serializeList(context, blockHeaderenMsgs , byteWriter);
    }


    /**
     * Deserialize BlockHeaderEnrichedMsg list
     *
     * @param context
     * @param byteReader
     * @return
     */
    protected List<BlockHeaderEnrichedMsg> deserializeBlockHeaderenMsgs(DeserializerContext context, ByteArrayReader byteReader) {
        VarIntMsg count = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        BlockHeaderEnrichedMsg blockHeaderenMsg;
        List<BlockHeaderEnrichedMsg> blockHeaderenMsgs = new ArrayList<>();

        BlockHeaderEnMsgSerializer blockHeaderEnMsgSerializer = BlockHeaderEnMsgSerializer.getInstance();
        for(int i =0 ; i < count.getValue(); i++) {
            blockHeaderenMsg = blockHeaderEnMsgSerializer.deserialize(context, byteReader);
            blockHeaderenMsgs.add(blockHeaderenMsg);
        }

        return blockHeaderenMsgs;
    }

    /**
     * Serialize BlockHeaderEnrichedMsg List
     * @param context
     * @param blockHeaderEnrichedMsg
     * @param byteWriter
     */
    protected void serializeList(SerializerContext context, List<BlockHeaderEnrichedMsg> blockHeaderEnrichedMsg, ByteArrayWriter byteWriter) {
        for (BlockHeaderEnrichedMsg blockHeaderenMsg:blockHeaderEnrichedMsg) {
            BlockHeaderEnMsgSerializer.getInstance().serialize(context, blockHeaderenMsg, byteWriter);
        }
    }
}
