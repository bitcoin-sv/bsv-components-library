package com.nchain.jcl.protocol.serialization;

import com.nchain.jcl.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.protocol.messages.PartialBlockHeaderMsg;
import com.nchain.jcl.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 12:48
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
        BlockHeaderMsg blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, reader);
        return PartialBlockHeaderMsg.builder().blockHeader(blockHeader).build();
    }

    @Override
    public void serialize(SerializerContext context, PartialBlockHeaderMsg msg, ByteArrayWriter writer) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, msg.getBlockHeader(), writer);
    }

}
