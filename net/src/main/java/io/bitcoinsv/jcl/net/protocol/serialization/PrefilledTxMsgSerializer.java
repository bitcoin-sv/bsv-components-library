/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.PrefilledTxMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class PrefilledTxMsgSerializer implements MessageSerializer<PrefilledTxMsg> {
    private static PrefilledTxMsgSerializer instance;

    public static PrefilledTxMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (PrefilledTxMsgSerializer.class) {
                instance = new PrefilledTxMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public PrefilledTxMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        var index = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        var transaction = TxMsgSerializer.getInstance().deserialize(context, byteReader);

        return PrefilledTxMsg.builder()
                .index(index)
                .transaction(transaction)
                .build();
    }

    @Override
    public void serialize(SerializerContext context, PrefilledTxMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getIndex(), byteWriter);
        TxMsgSerializer.getInstance().serialize(context, message.getTransaction(), byteWriter);
    }
}
