package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HeadersMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

import java.util.ArrayList;
import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 20/08/2019
 *
 *  A Serializer for {@link HeadersMsg} messages
 */
public class HeadersMsgSerializer implements MessageSerializer<HeadersMsg> {

    private static HeadersMsgSerializer instance;

    private HeadersMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
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

        BlockHeaderMsgSerializer blockHeaderMsgSerializer = BlockHeaderMsgSerializer.getInstance();
        for(int i =0 ; i < count.getValue(); i++) {
            blockHeaderMsg = blockHeaderMsgSerializer.deserialize(context, byteReader);
            blockHeaderMsgs.add(blockHeaderMsg);
        }

        return blockHeaderMsgs;
    }

    @Override
    public void serialize(SerializerContext context, HeadersMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        List<BlockHeaderMsg> blockHeaderMsg = message.getBlockHeaderMsgList();
        serializeList(context, blockHeaderMsg , byteWriter);
    }

    /**
     * Serialize blockHeadersMsg List
     * @param context
     * @param blockHeaderMsgList
     * @param byteWriter
     */
    protected void serializeList(SerializerContext context, List<BlockHeaderMsg> blockHeaderMsgList, ByteArrayWriter byteWriter) {
        for (BlockHeaderMsg blockHeaderMsg:blockHeaderMsgList) {
            BlockHeaderMsgSerializer.getInstance().serialize(context, blockHeaderMsg, byteWriter);
        }
    }
}
