package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.BlockTxnMsg;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class BlockTxnMsgSerializer implements MessageSerializer<BlockTxnMsg> {
    private static BlockTxnMsgSerializer instance;

    public static BlockTxnMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (BlockTxnMsgSerializer.class) {
                instance = new BlockTxnMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockTxnMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        var blockHash = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        var transactionLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);

        context.setCalculateHashes(true);

        List<TxMsg> transactions = new ArrayList<>();
        for (int i = 0; i < transactionLength.getValue(); i++) {
            transactions.add(TxMsgSerializer.getInstance().deserialize(context, byteReader));
        }

        return BlockTxnMsg.builder()
            .blockHash(blockHash)
            .transactions(transactions)
            .build();
    }

    @Override
    public void serialize(SerializerContext context, BlockTxnMsg message, ByteArrayWriter byteWriter) {
        HashMsgSerializer.getInstance().serialize(context, message.getBlockHash(), byteWriter);

        VarIntMsgSerializer.getInstance().serialize(
            context,
            VarIntMsg.builder()
                .value(message.getTransactions().size())
                .build(),
            byteWriter
        );

        var txMsgSerializer = TxMsgSerializer.getInstance();
        message.getTransactions().forEach(transaction -> txMsgSerializer.serialize(context, transaction, byteWriter));
    }
}
