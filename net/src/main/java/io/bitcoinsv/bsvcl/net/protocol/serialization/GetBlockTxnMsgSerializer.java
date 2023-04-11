package io.bitcoinsv.bsvcl.net.protocol.serialization;

import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.GetBlockTxnMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class GetBlockTxnMsgSerializer implements MessageSerializer<GetBlockTxnMsg> {
    private static GetBlockTxnMsgSerializer instance;

    public static GetBlockTxnMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (GetBlockTxnMsgSerializer.class) {
                instance = new GetBlockTxnMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public GetBlockTxnMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        var blockHash = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        var indexesLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);

        List<VarIntMsg> indexes = new ArrayList<>();
        for (int i = 0; i < indexesLength.getValue(); i++) {
            indexes.add(VarIntMsgSerializer.getInstance().deserialize(context, byteReader));
        }

        return GetBlockTxnMsg.builder()
            .blockHash(blockHash)
            .indexesLength(indexesLength)
            .indexes(indexes)
            .build();
    }

    @Override
    public void serialize(SerializerContext context, GetBlockTxnMsg message, ByteArrayWriter byteWriter) {
        HashMsgSerializer.getInstance().serialize(context, message.getBlockHash(), byteWriter);
        var varIntMsgSerializer = VarIntMsgSerializer.getInstance();
        varIntMsgSerializer.serialize(context, message.getIndexesLength(), byteWriter);
        message.getIndexes().forEach(index -> varIntMsgSerializer.serialize(context, index, byteWriter));
    }
}
