/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

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
                .txsSizeInBytes(context.getMaxBytesToRead() - blockHeader.getLengthInBytes())
                .build();
    }

    @Override
    public void serialize(SerializerContext context, PartialBlockHeaderMsg msg, ByteArrayWriter writer) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, msg.getBlockHeader(), writer);
    }

}
