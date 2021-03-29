package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HeadersMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * A Serializer for {@link HeadersMsg} messages
 */
public class HeadersMsgSerializer implements MessageSerializer<HeadersMsg> {

    private static HeadersMsgSerializer instance;

    private HeadersMsgSerializer() {
    }

    /**
     * Returns an instance of this Serializer (Singleton)
     */
    public static HeadersMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (HeadersMsgSerializer.class) {
                instance = new HeadersMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public HeadersMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        List<BlockHeaderMsg> blockHeaderMsgs = deserializeList(context, byteReader);
        HeadersMsg headersMsg = HeadersMsg.builder().blockHeaderMsgList(blockHeaderMsgs).build();

        return headersMsg;
    }

    /**
     * Deserialize blockHeadersMsg list
     *
     * @param context
     * @param byteReader
     * @return
     */
    protected List<BlockHeaderMsg> deserializeList(DeserializerContext context, ByteArrayReader byteReader) {
        VarIntMsg count = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        BlockHeaderMsg blockHeaderMsg;
        List<BlockHeaderMsg> blockHeaderMsgs = new ArrayList<>();

        var blockHeaderMsgSerializer = CompleteBlockHeaderMsgSerializer.getInstance();
        for (int i = 0; i < count.getValue(); i++) {
            blockHeaderMsg = blockHeaderMsgSerializer.deserialize(context, byteReader);
            blockHeaderMsgs.add(blockHeaderMsg);
        }

        return blockHeaderMsgs;
    }

    @Override
    public void serialize(SerializerContext context, HeadersMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        List<BlockHeaderMsg> blockHeaderMsg = message.getBlockHeaderMsgList();
        serializeList(context, blockHeaderMsg, byteWriter);
    }

    /**
     * Serialize blockHeadersMsg List
     *
     * @param context
     * @param blockHeaderMsgList
     * @param byteWriter
     */
    protected void serializeList(SerializerContext context, List<BlockHeaderMsg> blockHeaderMsgList, ByteArrayWriter byteWriter) {
        for (var blockHeaderMsg : blockHeaderMsgList) {
            CompleteBlockHeaderMsgSerializer.getInstance().serialize(context, blockHeaderMsg, byteWriter);
        }
    }
}
