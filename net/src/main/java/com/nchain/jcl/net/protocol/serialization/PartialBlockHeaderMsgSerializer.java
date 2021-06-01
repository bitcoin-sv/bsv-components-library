package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.PartialBlockHeaderMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class PartialBlockHeaderMsgSerializer implements MessageSerializer<PartialBlockHeaderMsg> {

    private static PartialBlockHeaderMsgSerializer instance;

    private PartialBlockHeaderMsgSerializer() {}

    public static PartialBlockHeaderMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (PartialBlockHeaderMsgSerializer.class) {
                instance = new PartialBlockHeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public PartialBlockHeaderMsg deserialize(DeserializerContext context, ByteArrayReader reader) {
        var blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, reader);
        return PartialBlockHeaderMsg.builder()
                .blockHeader(blockHeader)
                .blockSizeInBytes(context.getMaxBytesToRead())
                .build();
    }

    @Override
    public void serialize(SerializerContext context, PartialBlockHeaderMsg msg, ByteArrayWriter writer) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, msg.getBlockHeader(), writer);
    }

}
