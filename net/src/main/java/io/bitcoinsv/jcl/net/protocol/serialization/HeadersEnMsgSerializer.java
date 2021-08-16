package io.bitcoinsv.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.*;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderEnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HeadersEnMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer for {@link HeadersEnMsg} messages
 */
public class HeadersEnMsgSerializer implements MessageSerializer<HeadersEnMsg> {
    private static HeadersEnMsgSerializer instance;

    private HeadersEnMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static HeadersEnMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (HeadersEnMsgSerializer.class) {
                instance = new HeadersEnMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public HeadersEnMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        List<BlockHeaderEnMsg> blockHeaderEnMsgs = deserializeBlockHeaderenMsgs(context, byteReader);
        HeadersEnMsg headersenMsg = HeadersEnMsg.builder().blockHeaderEnMsgList(blockHeaderEnMsgs).build();
        return headersenMsg;
    }

    @Override
    public void serialize(SerializerContext context, HeadersEnMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        List<BlockHeaderEnMsg> blockHeaderenMsgs = message.getBlockHeaderEnMsgList();
        serializeList(context, blockHeaderenMsgs , byteWriter);
    }


    /**
     * Deserialize BlockHeaderEnMsg list
     *
     * @param context
     * @param byteReader
     * @return
     */
    protected List<BlockHeaderEnMsg> deserializeBlockHeaderenMsgs(DeserializerContext context, ByteArrayReader byteReader) {
        VarIntMsg count = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        BlockHeaderEnMsg blockHeaderenMsg;
        List<BlockHeaderEnMsg> blockHeaderenMsgs = new ArrayList<>();

        BlockHeaderEnMsgSerializer blockHeaderEnMsgSerializer = BlockHeaderEnMsgSerializer.getInstance();
        for(int i =0 ; i < count.getValue(); i++) {
            blockHeaderenMsg = blockHeaderEnMsgSerializer.deserialize(context, byteReader);
            blockHeaderenMsgs.add(blockHeaderenMsg);
        }

        return blockHeaderenMsgs;
    }

    /**
     * Serialize BlockHeaderEnMsg List
     * @param context
     * @param blockHeaderEnMsg
     * @param byteWriter
     */
    protected void serializeList(SerializerContext context, List<BlockHeaderEnMsg> blockHeaderEnMsg, ByteArrayWriter byteWriter) {
        for (BlockHeaderEnMsg blockHeaderenMsg: blockHeaderEnMsg) {
            BlockHeaderEnMsgSerializer.getInstance().serialize(context, blockHeaderenMsg, byteWriter);
        }
    }
}
