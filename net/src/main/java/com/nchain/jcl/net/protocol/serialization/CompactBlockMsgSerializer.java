package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.CompactBlockMsg;
import com.nchain.jcl.net.protocol.messages.PrefilledTxMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class CompactBlockMsgSerializer implements MessageSerializer<CompactBlockMsg> {
    private static CompactBlockMsgSerializer instance;

    public static CompactBlockMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (CompactBlockMsgSerializer.class) {
                instance = new CompactBlockMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public CompactBlockMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        // First we deserialize the Block Header:
        BlockHeaderMsg blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);

        long nonce = byteReader.readInt64LE();

        // read number of short transaction id and short transaction ids
        var shortIdsLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        var shortTxIds = new long[(short) shortIdsLength.getValue()];
        for (int i = 0; i < shortTxIds.length; i++) {
            shortTxIds[i] = Utils.readInt64(byteReader.read(6), 0);
        }

        // read number of prefilled transactions and transactions
        var prefilledTxnLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        var prefilledTransactions = new PrefilledTxMsg[(short) prefilledTxnLength.getValue()];
        for (int i = 0; i < prefilledTransactions.length; i++) {
            prefilledTransactions[i] = PrefilledTxMsgSerializer.getInstance().deserialize(context, byteReader);
        }

        return CompactBlockMsg.builder()
            .header(blockHeader)
            .nonce(nonce)
            .shortTxIds(shortTxIds)
            .prefilledTransactions(prefilledTransactions)
            .build();
    }

    @Override
    public void serialize(SerializerContext context, CompactBlockMsg message, ByteArrayWriter byteWriter) {
        // write header
        BlockHeaderMsgSerializer.getInstance().serialize(context, message.getHeader(), byteWriter);

        // write nonce
        byteWriter.writeUint64LE(message.getNonce());

        var shortTxIds = message.getShortTxIds();

        // write short transactions length
        var shortTxIdsLength = VarIntMsg.builder().value(shortTxIds.length).build();
        VarIntMsgSerializer.getInstance().serialize(context, shortTxIdsLength, byteWriter);

        var buffer = ByteBuffer.allocate(6);
        Arrays.stream(shortTxIds).forEach(shortTxId -> {
            buffer.clear();
            buffer.putLong(shortTxId);
            buffer.flip();
            byteWriter.write(buffer.array());
        });

        var prefilledTransactions = message.getPrefilledTransactions();

        // write short transactions length
        var txIdsLength = VarIntMsg.builder().value(prefilledTransactions.length).build();
        VarIntMsgSerializer.getInstance().serialize(context, txIdsLength, byteWriter);

        Arrays.stream(prefilledTransactions).forEach(prefilledTransaction ->
            PrefilledTxMsgSerializer.getInstance().serialize(context, prefilledTransaction, byteWriter)
        );
    }
}
